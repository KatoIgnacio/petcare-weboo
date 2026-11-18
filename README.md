# PetCare

Avance de desarrollo del sistema de gestión de atención veterinaria para la asignatura Programación Web Orientada a Objetos.

## Estado del avance

- Backend Spring Boot 4.1.1 con Java 21.
- Arquitectura `Controller -> Service -> Repository`.
- Modelo relacional para clientes, mascotas, veterinarios, reservas y atenciones médicas.
- PostgreSQL mediante Docker Compose.
- Validación de datos y manejo uniforme de errores HTTP.
- Prevención de reservas duplicadas para el veterinario o la mascota.
- Estados `PENDIENTE`, `CONFIRMADA`, `CANCELADA` y `ATENDIDA`.
- Servicio Node para medicamentos, ubicación configurable y envío SMTP de notificaciones, en el repositorio hermano `petcare-external-api`.
- Avisos persistidos en PostgreSQL, enviados después del commit y recuperados con reintentos limitados.
- Pruebas automatizadas del flujo principal.
- Flujo integrado verificado con PostgreSQL 17 y la API externa en ejecución.
- Listado de clientes activos, agenda filtrada e historial por mascota, incluida la consulta después de una baja lógica.
- Frontend en HTML, CSS y JavaScript servido desde Spring Boot para gestionar clientes, mascotas, veterinarios, reservas y atenciones.

## Puertos

| Componente | Puerto |
|---|---:|
| Backend Spring Boot | 8082 |
| API externa Node | 3001 |
| PostgreSQL | 5433 |
| SMTP local Mailpit | 1025 |
| Mensajes capturados Mailpit | 8025 |

## Estructura principal

```text
WebOO/
|- petcare-weboo/
|  |- src/main/java/cl/bello/petcare/
|  |  |- controller/
|  |  |- service/
|  |  |- repository/
|  |  |- entity/
|  |  |- dto/
|  |  |- exception/
|  |  `- config/
|  |- src/main/resources/
|  |  |- static/
|  |  |- schema.sql
|  |  |- data.sql
|  |  `- application.properties
|  |- docs/
|  |- postman/
|  `- compose.yaml
`- petcare-external-api/
   |- server.js
   |- package.json
   `- README.md
```

## Ejecución

Se requiere Java 21, Node.js 20 o posterior y Docker Desktop. Los comandos de PostgreSQL y Spring se ejecutan desde la raíz de `petcare-weboo`. El repositorio `petcare-external-api` debe estar en la carpeta hermana indicada en la estructura.

### 1. Iniciar Docker Desktop

Esperar hasta que el motor de Docker aparezca activo.

### 2. Iniciar PostgreSQL

```powershell
docker compose up -d postgres
docker compose ps
```

El contenedor `petcare-postgres` debe aparecer como `healthy`.

### 3. Iniciar la API externa

En una segunda terminal, comenzando desde la raíz de `petcare-weboo`, ejecutar

```powershell
Set-Location ..\petcare-external-api
npm.cmd ci
docker compose up -d mailpit
$env:HOST = '127.0.0.1'
$env:PORT = '3001'
$env:SMTP_HOST = '127.0.0.1'
$env:SMTP_PORT = '1025'
$env:SMTP_SECURE = 'false'
$env:SMTP_REQUIRE_TLS = 'false'
$env:SMTP_FROM = 'avisos@petcare.example.invalid'
npm.cmd start
```

Se debe mostrar `PetCare external API disponible`.

### 4. Iniciar Spring Boot

En una tercera terminal, desde la raíz de `petcare-weboo`, seleccionar el JDK 21 instalado y ejecutar. La ruta siguiente corresponde al equipo de desarrollo y debe ajustarse si la instalación de Java está en otra ubicación.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:EXTERNAL_API_URL = 'http://localhost:3001'
.\gradlew.bat bootRun
```

Abre [PetCare](http://localhost:8082/) para entrar a la agenda clínica. Spring Boot sirve el frontend y atiende las operaciones de gestión y la consulta de avisos.

### Configuración de la API externa

`EXTERNAL_API_URL` define la URL base de Node. Su valor predeterminado es `http://localhost:3001`. El frontend obtiene la misma base de `GET /api/configuracion`, que devuelve únicamente `externalApiUrl`. El navegador consulta medicamentos y ubicación en Node y Spring solicita el envío de avisos al mismo servicio.

Asigna la variable antes de iniciar Spring. Para la demostración en red, usa la IP del computador que ejecuta Node, por ejemplo `http://192.168.1.25:3001`. Node debe escuchar en una interfaz alcanzable desde el otro equipo, por ejemplo con `HOST=0.0.0.0` en la red de demostración. Tanto el navegador de PetCare como Spring deben poder conectar con esa dirección.

Las variables se asignan en la terminal que inicia cada proceso. Spring lee `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `EXTERNAL_API_URL` y `CORS_ALLOWED_ORIGINS` según `application.properties`. Node lee `process.env`, incluidos sus parámetros SMTP y de clínica. Su README documenta también la carga explícita de `.env` mediante `node --env-file=.env server.js`, disponible desde Node 20.6.

Para repetir verificaciones sobre la base separada del diagnóstico, asignar también la siguiente variable en la terminal de Spring antes de iniciar la aplicación.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5433/petcare_diagnostico_20261117'
```

### Probar el frontend con una base separada

Desde la raíz de `petcare-weboo`, con PostgreSQL activo, prepara una base de prueba la primera vez. En este equipo la etapa 5 ya creó `petcare_etapa5_20261117`. Elige otro nombre si quieres preparar un conjunto nuevo.

```powershell
docker exec petcare-postgres createdb -U postgres petcare_etapa5_pruebas
```

En la terminal de Spring, selecciona Java 21 y la base creada antes de usar `bootRun`.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5433/petcare_etapa5_pruebas'
.\gradlew.bat bootRun
```

En [PetCare](http://localhost:8082/), entra a **Clientes y mascotas**, registra un cliente y agrega una mascota. Crea o elige un veterinario y usa **Nueva reserva** en **Agenda**. Abre el detalle para reprogramar, confirmar, cancelar o registrar atención según el estado. El historial se puede abrir desde la mascota o desde el detalle de una reserva, incluso después de una baja lógica.

### Probar la integración con Node y Mailpit

1. Inicia Node y Mailpit con la configuración de desarrollo de arriba.
2. En **Medicamentos**, busca `amox` para ver el registro de ejemplo. En **Clínica**, revisa la dirección y la acción **Abrir mapa**. Para mostrar el origen de las respuestas, abre la pestaña **Red** de las herramientas del navegador y observa el puerto de las solicitudes a medicamentos y ubicación.
3. Confirma o cancela una reserva de prueba y observa su aviso en el detalle. La consulta se actualiza mientras el detalle está abierto y el aviso está en curso. Usa **Actualizar detalle** si quieres volver a comprobarlo manualmente.
4. Busca el aviso capturado en [Mailpit](http://localhost:8025/).

En el entorno de demostración, usa `CLINIC_NAME`, `CLINIC_ADDRESS`, `CLINIC_LATITUDE` y `CLINIC_LONGITUDE` en Node para configurar los datos reales. Las ubicaciones y medicamentos de ejemplo se identifican en la interfaz. [Informe de la etapa 6](docs/integracion-frontend-etapa6.md)

## Endpoints del reporte

| ID | Método | URL |
|---|---|---|
| E01 | POST | `/api/clientes` |
| E02 | POST | `/api/clientes/{clienteId}/mascotas` |
| E03 | GET | `/api/clientes/{clienteId}/mascotas` |
| E04 | POST | `/api/reservas` |
| E05 | PUT | `/api/reservas/{id}` |
| E06 | PATCH | `/api/reservas/{id}/estado` |
| E07 | POST | `/api/reservas/{id}/atencion` |
| E08 | GET | `http://localhost:3001/api/medicamentos?nombre={nombre}` |
| E09 | GET | `http://localhost:3001/api/ubicacion-clinica` |
| E10 | POST | `http://localhost:3001/api/notificaciones` |

También se implementaron rutas auxiliares para consultar, actualizar y desactivar clientes y mascotas, crear veterinarios y consultar una reserva.

## Consultas para el frontend

| Método y ruta | Parámetros | Respuesta 200 |
|---|---|---|
| `GET /api/configuracion` | Configuración pública del servicio externo | Objeto con `externalApiUrl` |
| `GET /api/clientes` | Consulta completa de clientes activos | Arreglo de ClienteResponse, ordenado por nombre e id ascendentes |
| `GET /api/reservas` | `fecha` y `veterinarioId` opcionales | Arreglo de ReservaAgendaResponse, ordenado por fechaHora e id ascendentes |
| `GET /api/mascotas/{id}/historial` | Identificador de mascota en la ruta | Arreglo de AtencionResponse, ordenado por fecha e id descendentes |

Los listados devuelven `[]` cuando la consulta válida carece de resultados. Un historial de mascota inexistente devuelve 404. El historial admite mascotas dadas de baja y la agenda conserva las referencias históricas de clientes, mascotas y profesionales.

La agenda devuelve `id`, `mascotaId`, `mascotaNombre`, `clienteId`, `clienteNombre`, `veterinarioId`, `veterinarioNombre`, `fechaHora` y `estado`.

`fecha` requiere una fecha válida `YYYY-MM-DD` y selecciona desde las 00:00 de ese día hasta antes de las 00:00 del día siguiente. `veterinarioId` requiere un entero positivo. Ambos filtros se aplican conjuntamente cuando se proporcionan. Las consultas aceptan fechas pasadas y futuras. Un identificador de veterinario válido sin coincidencias produce `[]`. Los filtros vacíos o inválidos producen 400.

Con Spring iniciado, ejecutar desde PowerShell

```powershell
Invoke-RestMethod 'http://localhost:8082/api/clientes'
Invoke-RestMethod 'http://localhost:8082/api/reservas?fecha=2026-11-17'
Invoke-RestMethod 'http://localhost:8082/api/reservas?fecha=2026-11-17&veterinarioId=1'
Invoke-RestMethod 'http://localhost:8082/api/mascotas/1/historial'
```

El último ejemplo utiliza la mascota 1. Para otros conjuntos de datos, utilizar el id obtenido al registrar o consultar la mascota.

### Fechas y errores HTTP

`fechaHora` se intercambia como hora local ISO sin zona, por ejemplo `2026-11-27T10:30:00`, y se almacena como TIMESTAMP mediante LocalDateTime. El equipo de desarrollo utiliza America/Santiago. La zona de la JVM debe corresponder a la hora local de la clínica. El frontend y otros consumidores deben enviar esa misma hora local.

Las reservas nuevas y sus modificaciones requieren fecha futura. La consulta de agenda permite recuperar registros históricos. El tratamiento UTC de la colección Postman está registrado como pendiente en el diagnóstico inicial.

Los errores usan ApiError con `timestamp`, `status`, `error`, `mensaje`, `ruta` y `detalles`. Se comprobaron 400 por datos o parámetros inválidos, 404 por recurso inexistente, 405 por método incompatible, 409 por conflicto y 415 por tipo de contenido incompatible. La respuesta 405 conserva la cabecera Allow. Los fallos internos inesperados reciben 500 con mensaje genérico.

## Notificaciones persistentes de la etapa 4

Confirmar o cancelar una reserva guarda un registro en `notificaciones_pendientes` dentro de la misma transacción. El trabajador periódico procesa los registros confirmados y realiza HTTP fuera de la transacción. La restricción única sobre reserva y estado coordina la creación de eventos. Los bloqueos breves, la concesión temporal y el token de intento coordinan trabajadores. Los tiempos operativos se guardan en UTC con TIMESTAMP WITH TIME ZONE. `fechaHora` conserva su interpretación local.

E06 conserva sus campos y devuelve inicialmente `resultadoNotificacion=PENDIENTE:<claveEvento>`. `GET /api/reservas/{id}` permite consultar el resultado persistido del último evento.

| Resultado | Significado |
|---|---|
| NO_APLICA | Operación sin evento nuevo, incluida la repetición del estado actual. Una reserva anterior sin eventos también devuelve este valor al consultarla |
| PENDIENTE:UUID | Evento confirmado, listo para su primer intento |
| EN_PROCESO:UUID | Trabajador con un intento en curso |
| PENDIENTE_REINTENTO:UUID | Fallo temporal y siguiente intento registrado |
| ACEPTADA:UUID | SMTP aceptó el mensaje y Node guardó el recibo. El UUID corresponde a idNotificacion |
| FALLIDA:UUID | Fallo definitivo o agotamiento de intentos con resultado conocido |
| INCIERTA:UUID | Resultado pendiente de revisión, con repetición automática detenida |

Los estados previos a ACEPTADA y los finales FALLIDA e INCIERTA incluyen la clave del evento. Una caída de Node o SMTP conserva el estado de la reserva y el aviso. Confirmación y cancelación tienen claves distintas. Repetir la transición actual devuelve NO_APLICA y conserva el evento existente.

El trabajador se activa al iniciar Spring y recupera pendientes de PostgreSQL. Cada intento reclama brevemente el aviso y libera la transacción antes de esperar la red. La misma clave se transmite a Node mediante `Idempotency-Key`. Los casos inciertos y los fallos definitivos requieren revisar el registro y la evidencia SMTP antes de decidir otro envío. Los cambios de estado anteriores a esta etapa conservan su historial existente.

| Variable Spring | Predeterminado | Uso |
|---|---|---|
| EXTERNAL_API_CONNECT_TIMEOUT | 2s | Espera de conexión HTTP |
| EXTERNAL_API_READ_TIMEOUT | 10s | Espera de respuesta HTTP |
| NOTIFICATION_MAX_ATTEMPTS | 5 | Máximo de intentos por evento |
| NOTIFICATION_RETRY_DELAY | 30s | Primera espera entre intentos |
| NOTIFICATION_MAX_RETRY_DELAY | 5m | Tope de la espera exponencial |
| NOTIFICATION_LEASE_DURATION | 30s | Concesión de un intento antes de su recuperación |
| NOTIFICATION_BATCH_SIZE | 10 | Máximo de avisos considerados en cada ciclo |
| NOTIFICATION_WORKER_ENABLED | true | Activa el trabajador periódico |
| NOTIFICATION_WORKER_DELAY_MS | 5000 | Intervalo entre ciclos, en ms |
| NOTIFICATION_WORKER_INITIAL_DELAY_MS | 5000 | Espera inicial, en ms |

La política predeterminada permite cinco intentos totales, con esperas de 30, 60, 120 y 240 segundos entre fallos temporales. SMTP tiene sus propios tiempos de espera documentados en el repositorio externo. Configurar la concesión teniendo en cuenta la duración máxima prevista del intento HTTP. Una concesión vencida permite recuperar un proceso interrumpido usando la misma clave. Un último intento interrumpido queda INCIERTA para revisión.

Para inspeccionar estados, errores y próximas ejecuciones en la base utilizada

```powershell
docker exec petcare-postgres psql -U postgres -d petcare_diagnostico_20261117 -c 'SELECT reserva_id, estado_reserva, estado, intentos, proximo_intento, ultimo_error FROM notificaciones_pendientes ORDER BY id;'
```

Los mensajes SMTP de desarrollo se revisan en [Mailpit local](http://localhost:8025). Allí se comprueban remitente, destinatario, asunto y cuerpo. La etapa 4 verificó aceptación y captura local. La entrega a una casilla externa requiere configuración del proveedor y un destinatario autorizado. Los contratos E08–E10 y la configuración completa están en el README del repositorio externo.

## Base de datos

`schema.sql` crea las tablas y restricciones. `data.sql` agrega un veterinario demostrativo:

- Email: `paula.rojas@petcare.cl`
- Especialidad: Medicina general

La eliminación de clientes y mascotas es lógica para conservar el historial.

### Datos de demostración ampliados

El archivo `database/demo-data.sql` agrega clientes, mascotas, veterinarios,
reservas en distintos estados, atenciones y medicamentos. Es idempotente: se
puede ejecutar nuevamente sin duplicar el conjunto de demostración.

```powershell
docker cp database/demo-data.sql petcare-postgres:/tmp/demo-data.sql
docker exec petcare-postgres psql -U postgres -d petcare -f /tmp/demo-data.sql
```

## Pruebas

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\gradlew.bat check
```

Las pruebas verifican:

- Carga del contexto y controladores.
- Creación HTTP de cliente, mascota, veterinario y reserva.
- Rechazo HTTP de una reserva duplicada.
- Confirmación de reserva y registro de atención médica.
- Listados, filtros de agenda, límites del día, orden del historial y acceso después de bajas lógicas.
- Códigos HTTP y formato ApiError, incluida la cabecera Allow y el tratamiento de fallos internos.

La etapa 3 verificó `26` pruebas automatizadas aprobadas y el empaquetado `bootJar`. También se realizaron 95 comprobaciones HTTP con PostgreSQL y cuatro escenarios de concurrencia. El informe contiene contratos, resultados y configuración utilizada. [Informe de la etapa 3](docs/backend-etapa3.md)

La etapa 4 añade pruebas de eventos transaccionales, rollback, recuperación, límites de reintentos, resultados inciertos y trabajadores concurrentes. Las pruebas Java utilizan H2 y mantienen desactivado el trabajador periódico para controlar cada ejecución. Node ejecuta su suite mediante `npm.cmd test` desde su repositorio. Los resultados finales y las evidencias SMTP están en el [informe de la etapa 4](docs/apis-externas-etapa4.md).

## Postman

Importar los dos archivos ubicados en `postman/`, seleccionar el entorno `PetCare Local` y ejecutar la colección completa en orden.

`baseUrl` apunta a Spring y `externalUrl` al servicio Node. El diagnóstico inicial documenta una aserción pendiente en ubicación y el tratamiento de fechas de la colección. Los resultados de la separación se encuentran en [el informe de la etapa 2](docs/separacion-api-externa.md).

## Nota sobre medicamentos

Los datos del servicio Node son demostrativos para probar la integración técnica. No constituyen indicaciones médicas ni reemplazan el criterio profesional veterinario.

