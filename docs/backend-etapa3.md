# Backend de PetCare, etapa 3

Trabajo realizado el 17 de noviembre de 2026, con hora local America/Santiago, UTC-03:00.

## Punto de partida y resultado

Se trabajó en `chore/diagnostico-inicial`, desde el commit `69712b6230fcb55b17acd56ff61703f9ea6a74bb`. El árbol de trabajo estaba limpio y se consultaron el diagnóstico inicial, el informe de separación y el código del backend. Los dos repositorios se utilizaron desde sus carpetas independientes.

La etapa incorporó el listado de clientes activos, la agenda con filtros y el historial por mascota. Se corrigió D02 mediante el manejo de excepciones MVC de Spring, conservando ApiError. También se añadió la comprobación explícita del cliente activo al crear y reprogramar reservas.

| Verificación | Resultado |
|---|---|
| Base previa a los cambios | 3 pruebas existentes aprobadas |
| Pruebas automatizadas finales | 26 aprobadas, 0 fallos, 0 errores y 0 omisiones |
| Pruebas nuevas de BackendEndpointsTests | 23 casos aprobados, incluidos 13 filtros inválidos parametrizados |
| Empaquetado bootJar | BUILD SUCCESSFUL |
| Comprobaciones HTTP con PostgreSQL y Node | 95 de 95 aprobadas |
| Escenarios de concurrencia con PostgreSQL | 4 de 4 aprobados |
| Conservación de registros anteriores al conjunto HTTP | Comprobada en las seis tablas de aplicación |

Los cambios de esta etapa quedan preparados para revisión local.

## Implementación y decisiones

| Componente | Cambio |
|---|---|
| ClienteController, ClienteService y ClienteRepository | Consulta de clientes activos con orden por nombre e id |
| ReservaController, ReservaService y ReservaRepository | Agenda con filtros opcionales, validación y consulta ordenada |
| ReservaAgendaResponse | DTO de nueve campos con datos de cliente, mascota, veterinario y reserva |
| MascotaController y MascotaService | Consulta de historial por mascota, incluidas bajas lógicas |
| AtencionMedicaRepository | Orden por fecha e id descendentes y carga conjunta de reserva y medicamentos |
| GlobalExceptionHandler | Uso de ResponseEntityExceptionHandler para conservar estados y cabeceras MVC y adaptar el cuerpo a ApiError |
| BackendEndpointsTests | Regresiones HTTP de consultas, errores y reglas de negocio con una base H2 propia |

La consulta de agenda proyecta directamente el DTO y obtiene los datos necesarios en una sentencia. El historial utiliza EntityGraph para cargar reserva y medicamentos dentro de una transacción de lectura. Ambas consultas funcionan con open-in-view desactivado.

Durante la comprobación con PostgreSQL se detectó que los parámetros opcionales usados en IS NULL requerían un tipo explícito. H2 había aceptado esa primera consulta. Se tiparon desde y hasta como LocalDateTime y veterinarioId como Long mediante CAST en HQL. Después se repitieron las pruebas automatizadas y las cuatro combinaciones de filtros con PostgreSQL satisfactoriamente.

Para D02 se adaptó el punto de creación de respuesta de ResponseEntityExceptionHandler. Spring conserva sus estados y cabeceras y la aplicación construye el formato ApiError. Los errores de validación y JSON mantienen sus mensajes específicos. Los conflictos de bloqueo optimista también se representan como 409. [Referencia de Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.html)

## Contratos de los endpoints nuevos

### GET /api/clientes

Devuelve 200 y un arreglo de clientes activos, con nombre ascendente e id ascendente como desempate. Cada elemento utiliza ClienteResponse.

```json
[
  {
    "id": 6,
    "nombre": "Etapa3 A",
    "email": "etapa3.ejemplo@example.invalid",
    "telefono": "+56911112222",
    "activo": true
  }
]
```

Un conjunto vacío produce `[]`. Las bajas lógicas quedan excluidas de este listado.

### GET /api/reservas

| Parámetro opcional | Formato | Comportamiento |
|---|---|---|
| fecha | YYYY-MM-DD con fecha válida | Incluye desde las 00:00 del día hasta antes de las 00:00 del siguiente |
| veterinarioId | Entero Long positivo | Selecciona las reservas de ese profesional |

Se puede consultar la agenda completa, aplicar un filtro o combinar ambos. El orden es fechaHora ascendente e id ascendente. Se admiten fechas pasadas y futuras, todos los estados y referencias históricas dadas de baja lógicamente. Un conjunto sin coincidencias devuelve 200 y `[]`, incluido un veterinarioId positivo inexistente.

Respuesta de ejemplo

```json
[
  {
    "id": 10,
    "mascotaId": 7,
    "mascotaNombre": "Etapa3 principal",
    "clienteId": 6,
    "clienteNombre": "Etapa3 A",
    "veterinarioId": 11,
    "veterinarioNombre": "Etapa3 principal",
    "fechaHora": "2026-12-08T11:00:00",
    "estado": "ATENDIDA"
  }
]
```

Los filtros proporcionados vacíos, fechas imposibles, formatos incorrectos, identificadores cero, negativos, fraccionarios o fuera del rango de Long producen 400 con ApiError.

### GET /api/mascotas/{id}/historial

Devuelve 200 y un arreglo de AtencionResponse, con fecha descendente e id descendente como desempate. Se comprueba la existencia de la mascota incluyendo los registros inactivos.

```json
[
  {
    "id": 5,
    "reservaId": 12,
    "fecha": "2026-01-02T10:00:00",
    "diagnostico": "Etapa3 diagnostico tecnico",
    "tratamiento": "Etapa3 demostracion",
    "medicamentos": ["Amoxicilina"],
    "observaciones": "Conjunto de pruebas"
  }
]
```

Una mascota existente sin atenciones produce `[]`. Una mascota inexistente produce 404. Se comprobó la separación entre historiales de mascotas diferentes y el acceso después de dar de baja a la mascota y a su cliente.

## Corrección de D02

| Solicitud o caso | Resultado final |
|---|---|
| GET /api/clientes | 200 con listado |
| GET /api/reservas | 200 con agenda |
| GET /api/mascotas/{id}/historial | 200 para mascota existente, 404 para inexistente |
| GET /api/etapa3-inexistente | 404 con ApiError |
| GET / mientras el frontend está pendiente | 404 con ApiError |
| GET /api/clientes/abc | 400 con ApiError |
| PUT /api/clientes | 405 con ApiError y Allow que contiene GET y POST |
| POST /api/clientes con text/plain | 415 con ApiError |
| POST /api/clientes con JSON inválido | 400 con ApiError |
| Conflictos de horario o estados | 409 |
| Excepción interna controlada en el contexto de prueba | 500 con mensaje genérico |

El caso 500 se verificó mediante un controlador incluido en el contexto de pruebas. La prueba comprueba que el cuerpo excluya el detalle interno de la excepción. Los códigos HTTP anteriores se verificaron mediante peticiones reales a la aplicación de pruebas o al backend con PostgreSQL, según el caso registrado.

## Reglas de negocio comprobadas

- Creación y modificación con fecha futura.
- Rechazo de fechas pasadas.
- Existencia y actividad de mascota y veterinario.
- Cliente activo al crear y reprogramar, incluida una mascota activa asociada a un cliente inactivo en datos heredados.
- Conflictos de hora para el mismo veterinario o la misma mascota en PENDIENTE o CONFIRMADA.
- Exclusión de la propia reserva al reprogramar y rechazo de un destino ocupado.
- Restricción de cambios para CANCELADA y ATENDIDA.
- Registro de atención solo en CONFIRMADA y una atención por reserva.
- Cambio a ATENDIDA al registrar atención.
- Cancelación que libera la hora.
- Conservación y consulta de registros históricos después de bajas lógicas.
- Solicitudes a Node al confirmar y cancelar, con resultado ACEPTADA e identificador de la simulación vigente.

La coincidencia de horario sigue siendo exacta sobre fechaHora. Los horarios de apertura y la duración de las consultas quedan sujetos a una definición funcional posterior.

## Fechas y compatibilidad

La aplicación usa LocalDateTime y columnas TIMESTAMP. El valor `2026-12-08T11:00:00` representa las 11:00 locales de la clínica y se intercambia en ISO sin offset. El equipo utilizado tiene la zona Windows Pacific SA Standard Time, correspondiente a Santiago. La JVM debe ejecutarse con la zona local de la clínica para que las validaciones de fecha futura y el registro de fechas sean coherentes.

La fecha de agenda es un LocalDate independiente y acepta consultas históricas. Se probaron el inicio exacto del día, los últimos microsegundos del día y la exclusión de las 00:00 del siguiente. La persistencia existente se validó al iniciar Spring con PostgreSQL.

El desplazamiento UTC de la colección Postman continúa registrado como pendiente de etapa 7. Los consumidores deben enviar la hora local acordada al backend.

## Pruebas automatizadas

Se mantuvieron las tres pruebas de PetcareApplicationTests y se añadieron 23 casos en BackendEndpointsTests. Las regresiones nuevas de endpoints utilizan HTTP con puerto aleatorio y una base H2 separada llamada petcare_etapa3. La preparación de datos usa los repositorios y SQL únicamente sobre esa base de pruebas.

Cobertura de los casos nuevos

- Clientes activos, orden con nombres repetidos y resultados vacíos.
- Agenda sin filtros y con las cuatro combinaciones de filtros.
- Fechas pasadas, límites del día, todos los estados y referencias inactivas.
- Trece entradas inválidas parametrizadas.
- Historial con varias atenciones, empate de fechas, medicamentos y observaciones nulas.
- Historial vacío, mascota inexistente y separación entre mascotas.
- Acceso después de bajas lógicas de mascota y cliente.
- ApiError, Allow y códigos 400, 404, 405, 415 y 500.
- Cliente inactivo con mascota activa.
- Duplicidad, modificación propia, transiciones, atención y cancelación.
- Referencias inactivas de veterinario y mascota.

Los XML finales registran 23 casos aprobados de BackendEndpointsTests y 3 de PetcareApplicationTests. La última ejecución completó check y bootJar con BUILD SUCCESSFUL. Los reportes están en `build/test-results/test` y `build/reports/tests/test/index.html`.

## Integración HTTP con PostgreSQL y Node

Se realizaron 95 comprobaciones satisfactorias sobre `petcare_diagnostico_20261117` y el Node de `petcare-external-api`.

El recorrido incluyó alta de cliente, mascotas y veterinarios, reserva, modificación, confirmación, atención, consulta de historial, cancelación, nueva reserva en hora liberada y bajas lógicas. Se comprobaron los endpoints nuevos, sus filtros y los errores de D02. La agenda se validó con datos antiguos y con datos nuevos ubicados en los límites del día.

Los conjuntos HTTP nuevos usan nombres Etapa3 y correos example.invalid. Las atenciones y reservas utilizadas para ordenar historiales se ajustaron exclusivamente dentro de esos conjuntos identificados. Se creó temporalmente el caso heredado de mascota activa y cliente inactivo para verificar su rechazo y se restauró la baja de esa mascota al terminar la comprobación.

Se comparó cada registro previo al conjunto HTTP con su estado posterior en las seis tablas. La comparación fue satisfactoria.

| Tabla | Registros previos conservados | Total posterior |
|---|---|---|
| clientes | 5 | 10 |
| mascotas | 6 | 11 |
| veterinarios | 6 | 12 |
| reservas | 9 | 26 |
| atenciones_medicas | 2 | 8 |
| atencion_medicamentos | 2 | 8 |

## Concurrencia con PostgreSQL

Cada caso lanzó dos solicitudes HTTP simultáneas. Se comprobó su solapamiento temporal y se consultó PostgreSQL para validar el registro final.

| Caso | Respuestas | Verificación SQL |
|---|---|---|
| Mismo veterinario y hora, mascotas diferentes | 201 y 409 | Una reserva activa del veterinario en esa hora |
| Misma mascota y hora, veterinarios diferentes | 201 y 409 | Una reserva activa de la mascota en esa hora |
| Dos reprogramaciones al mismo veterinario y hora | 200 y 409 | Una reserva activa en el destino |
| Dos registros de atención sobre la misma reserva confirmada | 201 y 409 | Una atención para la reserva |

Los cuatro casos pasaron utilizando los bloqueos y transacciones del proyecto. La comprobación corresponde a estas operaciones concurrentes y registra los resultados de cada pareja de solicitudes.

## Configuración y reproducción

Desde la raíz de petcare-weboo, con Docker activo

```powershell
docker compose up -d postgres
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar check bootJar --no-daemon --console=plain
```

Desde una segunda terminal iniciada en esa misma raíz

```powershell
Set-Location ..\petcare-external-api
$env:HOST = '127.0.0.1'
$env:PORT = '3001'
npm.cmd start
```

Para iniciar Spring sobre la base de diagnóstico, desde petcare-weboo

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:DB_URL = 'jdbc:postgresql://localhost:5433/petcare_diagnostico_20261117'
$env:EXTERNAL_API_URL = 'http://localhost:3001'
$env:SERVER_PORT = '8082'
$env:SERVER_ADDRESS = '127.0.0.1'
& "$env:JAVA_HOME\bin\java.exe" -jar build/libs/petcare-0.0.1-SNAPSHOT.jar
```

Las credenciales utilizadas corresponden a la configuración local del Compose existente. Los ejemplos de Java requieren que JAVA_HOME esté seleccionado en la terminal correspondiente.

Con los servicios activos, se pueden revisar los resultados de esta ejecución

```powershell
Invoke-RestMethod 'http://localhost:8082/api/clientes'
Invoke-RestMethod 'http://localhost:8082/api/reservas?fecha=2026-12-12&veterinarioId=13'
Invoke-RestMethod 'http://localhost:8082/api/mascotas/7/historial'
```

La mascota 7 pertenece al conjunto de etapa 3 y está dada de baja lógicamente. Su historial tiene tres atenciones y permanece consultable. Los identificadores anteriores corresponden a esta base de diagnóstico concreta.

## Pendientes por etapa

| Etapa | Pendiente |
|---|---|
| 4 | Validación completa de notificaciones Node, envío real y representación de reintentos |
| 5 | Frontend HTML, CSS y JavaScript |
| 6 | Consumo de medicamentos y ubicación desde la interfaz y presentación de resultados de notificación |
| 7 | Ajustes de aserciones y fechas de Postman y verificación integral |
| 8 | Documentación final y demostración entre equipos |

La aceptación de notificaciones continúa siendo simulada en Node. El desarrollo del frontend y la demostración de red se realizarán en sus etapas correspondientes.

## Servicios y evidencia

Estado final comprobado a las 18:01, hora de Santiago

| Servicio | Uso en esta etapa | Estado de cierre |
|---|---|---|
| Docker Desktop y su motor | Ya estaban activos y se reutilizaron | Activos |
| PostgreSQL petcare-postgres | Ya estaba activo y se reutilizaron su volumen y la base de diagnóstico | Healthy en el puerto 5433 |
| Node del repositorio hermano | Iniciado mediante npm.cmd start para las pruebas | Detenido al terminar, puerto 3001 libre |
| Spring Boot | Iniciado y reiniciado para la verificación PostgreSQL | Detenido al terminar, puerto 8082 libre |

Se verificaron los identificadores y la línea de ejecución de los procesos propios antes de detenerlos. El repositorio externo quedó con árbol de trabajo limpio en develop, en el commit f75ddc1. El repositorio principal conserva los cambios de esta etapa pendientes de commit.

Evidencia local bajo build/etapa3

- `http-results.json` con las 95 solicitudes y respuestas.
- `concurrency-results.json` con las cuatro parejas, tiempos, códigos y recuentos SQL.
- `database-before.json` y `database-preservation.json` con la comprobación de conservación.
- `state.json` con los identificadores del conjunto.
- `node.stdout.log`, `node.stderr.log` y `spring-final.*.log` con los registros de ejecución.
- `services-start.json` y `services-final-start.json` con los procesos iniciados.
- `services-end.json` con la comprobación de cierre.

La evidencia local está en el directorio build ignorado por Git. Este documento conserva los contratos y resultados esenciales para revisar el trabajo.
