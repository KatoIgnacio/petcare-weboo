# PetCare: diagnóstico inicial

Fecha: 17 de noviembre de 2026. Zona horaria: America/Santiago (UTC-03:00).

## 1. Alcance y estado de la etapa

Se verificó el código existente sin reorganizar repositorios, implementar funcionalidades nuevas, modificar contratos HTTP ni actualizar versiones de dependencias.

La verificación inicial quedó realizada: se ejecutaron las pruebas existentes, se empaquetó la aplicación, se probaron los diez endpoints comprometidos y las nueve rutas auxiliares, y se reprodujeron escenarios de error con PostgreSQL real. Esto no significa que el producto completo cumpla todavía todos los requerimientos: los defectos y las funcionalidades pendientes se detallan más adelante.

| Comprobación | Resultado |
|---|---|
| Pruebas automatizadas existentes, con H2 | 3 ejecutadas, 3 aprobadas; 0 fallos, errores u omisiones |
| `check --rerun-tasks --no-daemon --console=plain` | BUILD SUCCESSFUL; 5 tareas ejecutadas |
| Empaquetado `bootJar` | BUILD SUCCESSFUL |
| Comprobaciones HTTP con PostgreSQL y Node | 84: 78 con resultado esperado y 6 que reprodujeron defectos |
| Diez endpoints del reporte, camino válido | Los diez respondieron según el contrato técnico comprobado |
| Solicitudes de la colección existente | 11 ejecutadas con un adaptador local; todas devolvieron el código esperado |
| Aserciones de la colección | 15 de 16 aprobadas; falla la comprobación de claves de ubicación |
| Envío real de correo o mensajería | No implementado; Node solo simula la aceptación |
| Comunicación entre dos computadores | No realizada; desarrollo y diagnóstico locales |

La colección se ejecutó con un adaptador propio limitado a las APIs `pm` que utiliza actualmente, usando las solicitudes y scripts originales. No se ejecutó el programa Postman ni Newman. Este resultado no se presenta como una ejecución oficial de esos programas.

## 2. Fuentes y estado de Git

Fuentes revisadas:

- Enunciado: `C:\Users\jaime\Desktop\Estudio de Caso 2.pdf`, 2 páginas.
- Reporte preliminar: `C:\Users\jaime\Desktop\PetCare_Caso_2.pdf`, 5 páginas.
- README, configuración Gradle, Docker Compose, SQL, código Java, servicio Node, pruebas y colección Postman.

Los PDF se utilizaron como especificaciones del proyecto. El enunciado define el alcance general; el reporte concreta RF01-RF11 y E01-E10. La decisión de dos repositorios también considera la instrucción del profesor comunicada por el usuario.

Estado al comenzar:

- Rama: `main`.
- Árbol de trabajo limpio.
- Commit de referencia: `c9c1e3ca46e1658abc8ce8c224f86f169bba80c7`.
- No se encontraron archivos `AGENTS.md` en los directorios ascendentes del proyecto ni dentro del repositorio.

Se creó la rama local `chore/diagnostico-inicial`. No se hicieron commits, push, publicaciones ni cambios en `main`.

El único entregable nuevo fuera de los directorios ignorados es este documento. Los scripts auxiliares de diagnóstico, registros, JSON de evidencia, compilación y reportes de pruebas están bajo `build/`, que ya está ignorado por Git. No se modificó código de producción ni la colección Postman.

## 3. Entorno utilizado

| Componente | Requisito declarado | Entorno comprobado |
|---|---|---|
| Sistema operativo | No se fija uno | Windows 11 Home Single Language, 64 bits, versión 10.0.26200 |
| Java | Toolchain 21 | Oracle JDK 21.0.12.1 seleccionado para esta ejecución |
| Java predeterminado del sistema | No aplica | JDK 25.0.4; no se cambió la configuración global |
| Gradle | Wrapper 9.7.1 | 9.7.1, ejecutado con Java 21 |
| Spring Boot | 4.1.1 en `build.gradle` | Compilación, pruebas y arranque satisfactorios |
| Node.js | >=20 | v24.15.0 |
| npm | No se fija versión | 11.12.1 mediante `npm.cmd` |
| Docker Engine | Necesario para Compose | 29.9.2 |
| Docker Desktop | Necesario para el motor local | 4.96.1 |
| Docker Compose | Necesario para PostgreSQL | v5.6.0 |
| PostgreSQL | Imagen `postgres:17-alpine` | PostgreSQL 17.11, contenedor healthy |
| H2 | Dependencia exclusiva de pruebas | Utilizado por las tres pruebas automatizadas |
| Cliente psql en Windows | No imprescindible | No se encontró en PATH; se utilizó el del contenedor |

Incidencias del entorno resueltas sin cambiar el proyecto:

1. PowerShell bloquea `npm.ps1` por su política de ejecución. `npm.cmd` funciona; no se cambió la política del equipo.
2. Docker Desktop estaba instalado, pero el motor no estaba iniciado. Se inició para poder ejecutar PostgreSQL.
3. El entorno aislado impedía algunas operaciones de Git, acceso a Docker y ejecución de Gradle. Se realizaron las operaciones autorizadas con el acceso necesario, sin cambios de políticas globales.
4. Se seleccionó Java 21 únicamente para los procesos de diagnóstico mediante `JAVA_HOME` y la ruta del ejecutable.

No se instalaron ni actualizaron frameworks o paquetes npm para esta etapa.

## 4. Puertos, procesos y aislamiento de datos

Al comenzar no se encontraron listeners en los puertos 8082, 3001, 5433 o 5432. Los procesos Java preexistentes identificados correspondían a herramientas de VS Code/Gradle; no se detuvieron. Tampoco se detuvieron procesos Node ajenos.

Docker tenía contenedores de otros proyectos detenidos. No se arrancaron, modificaron ni eliminaron.

Se inició el servicio PostgreSQL del Compose existente:

- Contenedor: `petcare-postgres`.
- Puerto publicado: 5433 hacia 5432 del contenedor.
- Se crearon por primera vez la red `petcare-weboo_default` y el volumen `petcare-weboo_petcare_data`; no se eliminó ni recreó ningún volumen existente.
- Base utilizada por Spring en el diagnóstico: `petcare_diagnostico_20261117`.
- La base `petcare` creada por el contenedor quedó sin tablas de aplicación: se comprobó un recuento de 0 tablas públicas.

Spring se inició con la configuración SQL existente sobre la base separada. Los casos HTTP emplearon registros propios, principalmente identificados con el prefijo `Diagnostico` y correos `example.invalid`. La colección conservó sus datos originales, siempre dentro de la misma base de diagnóstico. No se enviaron correos reales.

Configuración de los procesos HTTP durante la prueba:

- Spring: `127.0.0.1:8082`; `DB_URL` apuntó a la base de diagnóstico.
- Node: `HOST=127.0.0.1`, `PORT=3001`.
- Integración Spring a Node: `EXTERNAL_API_URL=http://localhost:3001`.
- Las pruebas de CORS comprobaron el origen Spring permitido `http://localhost:5173` y las cabeceras de Node para el frontend en `http://localhost:8082`.

La base de diagnóstico conserva 3 clientes, 4 mascotas, 4 veterinarios, 5 reservas y 2 atenciones médicas. Incluye registros dados de baja lógicamente. Los datos no se mezclaron con las bases de otros proyectos.

## 5. Pruebas automatizadas y empaquetado

Se ejecutó el wrapper existente con Java 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
& "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar check --rerun-tasks --no-daemon --console=plain
& "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar bootJar --no-daemon --console=plain
```

Resultados de `PetcareApplicationTests`:

| Prueba | Resultado |
|---|---|
| `cargaContextoYControladores()` | Aprobada |
| `endpointsCreanReservaYRechazanHorarioDuplicado()` | Aprobada |
| `reservaConfirmadaRegistraAtencionEHistorial()` | Aprobada |

Se comprobó el XML recién generado: 3 tests, 0 failures, 0 errors y 0 skipped. Se forzó su ejecución; el resultado no proviene de una ejecución antigua ni de tareas omitidas por caché.

Estas pruebas usan H2 en memoria con `create-drop`; no borran tablas de PostgreSQL. Su configuración apunta la API externa a `localhost:65535`, por lo que no acreditan la integración con Node ni un envío real de notificaciones. Además, el flujo completo de atención se prueba mediante servicios, mientras que la prueba HTTP existente comprueba creación y duplicidad.

El empaquetado produjo `build/libs/petcare-0.0.1-SNAPSHOT.jar`. Ese JAR se utilizó para las comprobaciones HTTP posteriores con PostgreSQL real y el SQL de producción.

## 6. Endpoints comprometidos verificados

Los identificadores se obtuvieron de las respuestas, no se asumieron valores preexistentes.

| ID | Método y ruta | Resultado comprobado |
|---|---|---|
| E01 | `POST /api/clientes` | 201, identificador, datos y Location; 400 por validación y 409 por email duplicado |
| E02 | `POST /api/clientes/{clienteId}/mascotas` | 201 y asociación al cliente; 400 por peso/nacimiento inválidos y 404 por cliente inexistente |
| E03 | `GET /api/clientes/{clienteId}/mascotas` | 200, mascotas del cliente y exclusión de bajas lógicas; 404 por cliente inexistente |
| E04 | `POST /api/reservas` | 201 y PENDIENTE; 400 por fecha pasada; 404 por referencias inexistentes/inactivas; 409 por duplicidad de veterinario o mascota |
| E05 | `PUT /api/reservas/{id}` | 200 al cambiar fecha/profesional; 409 por horario ocupado o reserva cancelada/atendida |
| E06 | `PATCH /api/reservas/{id}/estado` | 200 al confirmar/cancelar; aceptación simulada de notificación; 400 por estado inválido y 409 por transición incompatible |
| E07 | `POST /api/reservas/{id}/atencion` | 201 y medicamentos conservados; cambio a ATENDIDA; 400 por diagnóstico vacío y 409 por estado incompatible o atención repetida |
| E08 | `GET :3001/api/medicamentos?nombre=...` | 200 y arreglo con datos de medicamento; búsqueda parcial sin distinguir mayúsculas; 400 por nombre vacío y 404 sin resultados |
| E09 | `GET :3001/api/ubicacion-clinica` | 200 con nombre, dirección, coordenadas y urlMapa |
| E10 | `POST :3001/api/notificaciones` | 202 con idNotificacion y ACEPTADA; 400 por campos ausentes o JSON inválido; defectos de validación descritos en D01 |

El camino válido de E10 acredita la recepción técnica y respuesta de Node; no acredita entrega de un mensaje. Las respuestas 503 descritas para servicios externos no tienen una rama explícita en la implementación actual y no se reprodujeron como respuestas HTTP de Node. Cuando el proceso no está disponible existe un error de conexión, que Spring maneja según se explica más adelante.

## 7. Nueve rutas auxiliares verificadas

| Método y ruta | Resultado |
|---|---|
| `GET /api/clientes/{id}` | 200; 404 tras eliminación lógica o por inexistencia |
| `PUT /api/clientes/{id}` | 200 y cambios persistidos |
| `DELETE /api/clientes/{id}` | 204; desactiva también sus mascotas |
| `GET /api/mascotas/{id}` | 200; 404 por inexistencia/inactividad |
| `PUT /api/mascotas/{id}` | 200 y nuevo peso persistido |
| `DELETE /api/mascotas/{id}` | 204; deja de aparecer en el listado activo |
| `POST /api/veterinarios` | 201; 400 por datos inválidos y 409 por email duplicado |
| `GET /api/veterinarios` | 200 y profesionales activos |
| `GET /api/reservas/{id}` | 200 con detalle y atención; 404 por inexistencia |

Se comprobó que una atención sigue accesible desde el detalle de su reserva después de eliminar lógicamente al cliente. Esto verifica conservación del registro individual, no la existencia de una pantalla o endpoint de historial completo por mascota.

## 8. Flujo y caída de Node

Flujo verificado con PostgreSQL:

```text
Cliente → mascota → veterinario → reserva PENDIENTE
→ modificación → confirmación y aceptación simulada de aviso
→ atención médica → reserva ATENDIDA con datos de atención
```

También se comprobaron cancelaciones desde PENDIENTE y CONFIRMADA, rechazo de modificaciones de reservas finales y disponibilidad del horario después de cancelar.

Para la prueba de caída se detuvo exclusivamente el PID de Node creado por este diagnóstico, verificando antes que correspondía a `node.exe server.js`.

| Acción | Resultado observado |
|---|---|
| Consultar Node apagado | Servicio no accesible |
| Confirmar reserva con Node apagado | HTTP 200; estado CONFIRMADA y resultadoNotificacion PENDIENTE_REINTENTO |
| Consultar la reserva posteriormente | CONFIRMADA; el cambio quedó guardado |
| Repetir la confirmación | HTTP 200 y NO_APLICA; no vuelve a solicitar la notificación |
| Restaurar Node y cancelar | HTTP 200 y ACEPTADA con identificador simulado |

No se encontró una cola, tabla de avisos pendientes ni tarea que implemente reintentos. La caída se controla sin impedir el cambio de reserva, pero el resultado PENDIENTE_REINTENTO promete un comportamiento inexistente. Un GET posterior tampoco permite recuperar ese resultado de notificación.

## 9. Defectos reproducibles

### D01. Notificaciones aceptan valores nulos y tipos inválidos

Etapa de corrección: 4. Prioridad: alta para completar la API externa.

Reproducción: enviar a `POST http://localhost:3001/api/notificaciones` este JSON:

```json
{"destinatario":null,"canal":null,"asunto":null,"mensaje":null,"reservaId":null}
```

Resultado observado: 202, ACEPTADA e identificador. Resultado esperado: rechazo 400 porque no hay datos utilizables.

También devuelve 202 para:

```json
{"destinatario":123,"canal":"INVALIDO","asunto":[],"mensaje":{},"reservaId":-1}
```

Causa identificada en `external-api/server.js`: solo se consideran faltantes `undefined` y cadena vacía; no se comprueban nulos, tipos ni canal válido. Estos dos casos explican los dos fallos de la tanda HTTP principal. No se cambió la validación.

### D02. Excepciones HTTP normales se transforman en 500

Etapa de corrección: 3. Prioridad: alta para que la interfaz reciba errores correctos.

Reproducciones comprobadas:

| Solicitud | Esperado en el estado actual de rutas | Observado |
|---|---|---|
| `GET /api/clientes` | 405, porque actualmente solo existe POST sobre esa ruta base | 500 |
| `GET /api/reservas` | 405, por el mismo motivo | 500 |
| `GET /api/mascotas/{id}/historial` | 404, ruta aún no implementada | 500 |
| `GET /` | 404, sin frontend implementado | 500 |

Los cuatro devuelven el mensaje genérico de error interno. La captura global de `Exception` en `GlobalExceptionHandler` no conserva los estados propios de las excepciones de método no soportado y recurso no encontrado por Spring.

La corrección del manejo HTTP y la incorporación posterior de listados/frontend son trabajos distintos: tras implementar las rutas, sus GET válidos deberían devolver 200. No se implementaron durante este diagnóstico.

### D03. Aserción incorrecta en la colección Postman

Etapa de corrección: 7. Prioridad: media.

La solicitud de ubicación recibe 200 y estas cinco claves:

```text
nombre, direccion, latitud, longitud, urlMapa
```

La colección utiliza:

```javascript
pm.expect(pm.response.json()).to.have.keys('latitud', 'longitud')
```

Esa comparación exige el conjunto exacto de dos claves, mientras que la respuesta válida tiene cinco. Se reprodujo la discrepancia ejecutando el script original con el adaptador local. Debe comprobarse la inclusión de las coordenadas o el conjunto completo del contrato. La colección se dejó sin modificar.

### D04. La colección pierde la zona horaria y reutiliza datos temporales

Etapas de corrección: 3 para fijar el contrato horario y 7 para ajustar Postman. Prioridad: media.

El prerequest genera una fecha mediante `toISOString()` y luego elimina `Z`. Convierte la hora a UTC, pero después la envía como si fuera una hora local sin zona; Java la recibe como LocalDateTime.

En esta ejecución, una hora local aproximada de 00:55 en Santiago se convirtió en `2026-11-19T03:55:00.000` al generar la reserva de dos días después: desplazamiento de tres horas.

Además, `fechaReserva` y `sufijo` solo se inicializan cuando no existe `sufijo`. Reutilizar el entorno no garantiza una nueva fecha futura ni un email nuevo; puede producir 400 por fecha pasada o 409 por email duplicado. La primera ejecución funcionó con el entorno inicialmente vacío. La reutilización se identifica por lectura del script; no se presenta como una segunda corrida completa de la colección.

### D05. Notificación simulada y reintento inexistente

Etapa de corrección: 4. Prioridad: alta para cumplir el envío solicitado en el enunciado.

E10 genera un UUID y responde ACEPTADA, pero no usa correo, mensajería, persistencia de avisos ni entrega externa. La prueba de caída descrita en la sección 8 reproduce que repetir una confirmación no reintenta el aviso.

Debe implementarse el envío y representar honestamente aceptación, fallo y eventual reintento. El diagnóstico no propone una garantía de entrega ni afirma que haya llegado un correo.

## 10. Funcionalidades y documentación pendientes

| Pendiente respecto al sistema esperado | Etapa prevista |
|---|---|
| API Node aún dentro de `external-api` del repositorio principal | 2 |
| Listado general de clientes y consulta de agenda de reservas | 3 |
| Historial completo por mascota: hay consulta de repositorio sin servicio/controlador público | 3 |
| Envío real y validación completa de notificaciones | 4 |
| Frontend: no hay pantallas ni archivos estáticos de aplicación | 5 |
| Búsqueda de medicamentos y visualización de ubicación desde el frontend | 6 |
| Colección corregida y cobertura más amplia de errores/integración | 7 |
| README con rutas del equipo actual o instrucciones relativas | 8 |
| Instrucciones de ejecución entre dos equipos/redes | 8 y demostración posterior |

Observaciones adicionales:

- El catálogo Node contiene tres medicamentos demostrativos. La ubicación está fijada en código y no se verificó como dirección real de una clínica.
- E08 devuelve un arreglo. El reporte enumera campos pero no aclara expresamente si la salida es un objeto o una lista; debe documentarse la forma existente al completar el servicio, sin cambiarla silenciosamente.
- La disponibilidad se calcula por igualdad exacta de fecha y hora y estados PENDIENTE/CONFIRMADA. No hay duración de consulta ni calendario de apertura; no se considera un incumplimiento adicional sin definir esas reglas.
- Existen bloqueos pesimistas y control de versión en código. En esta etapa se comprobó duplicidad secuencial; no se acredita todavía una prueba de concurrencia ni de carga.
- El README contiene rutas personales de otro equipo (`C:\Users\KATO\Desktop\WebOS\petcare`). También afirma una integración previamente verificada; aquí se informan exclusivamente las comprobaciones nuevas realizadas en este equipo.
- El SQL demostrativo incluye reservas entre el 1 y el 11 de noviembre de 2026. Todas son anteriores a este diagnóstico; no deben utilizarse como solicitudes nuevas de reservas futuras sin adaptar los datos.
- No se verificó el requisito de segundo equipo/red. Cambiar puertos o ejecutar procesos independientes en una sola máquina no acredita ese requisito por sí solo.

## 11. Bloqueos reales y estado final de servicios

No quedaron bloqueos de entorno para continuar la etapa 2: Java 21, Gradle, Node, Docker y PostgreSQL pudieron utilizarse. Los defectos reproducidos son trabajo pendiente para etapas posteriores, no pruebas aprobadas.

La validación física entre equipos y el envío real de notificaciones continúan pendientes. Tampoco se ejecutaron pruebas de concurrencia, carga o una interfaz todavía inexistente; no son resultados acreditados por las tres pruebas automatizadas.

Estado de cierre comprobado a las 01:01, hora de Santiago:

- Docker Desktop y su motor quedaron activos; fueron iniciados durante esta etapa.
- PostgreSQL `petcare-postgres` quedó activo y healthy en el puerto 5433; fue iniciado durante esta etapa.
- Spring Boot fue iniciado para las pruebas y detenido al cerrar el diagnóstico. El puerto 8082 quedó sin listener.
- Node fue iniciado, detenido para la prueba de caída, restaurado y finalmente detenido. El puerto 3001 quedó sin listener.
- No se detuvieron procesos ajenos. Se verificó la identidad de los procesos propios antes de detenerlos.
- Se conservaron la base de diagnóstico, el volumen y los registros de evidencia.

Para revisar el resultado se puede abrir este documento o `build/reports/tests/test/index.html`. Para repetir las pruebas automatizadas, utilizar los comandos de la sección 5 desde la raíz del repositorio. Para repetir las comprobaciones HTTP, iniciar Node desde `external-api` y el JAR de Spring configurando `DB_URL` hacia la base de diagnóstico; no utilizar las pruebas HTTP sobre datos de terceros.

## 12. Evidencia local

Los siguientes archivos fueron generados durante esta ejecución y permanecen ignorados por Git:

- `build/test-results/test/TEST-cl.bello.petcare.PetcareApplicationTests.xml`: resultado de las tres pruebas existentes.
- `build/reports/tests/test/index.html`: reporte HTML de Gradle.
- `build/diagnostico/http-results.json`: 74 comprobaciones iniciales, con solicitudes y respuestas.
- `build/diagnostico/followup-results.json`: 10 comprobaciones adicionales, incluida caída y recuperación.
- `build/diagnostico/collection-results.json`: solicitudes y aserciones de la colección ejecutadas con adaptador local.
- `build/diagnostico/http-state.json`: identificadores del conjunto principal de diagnóstico.
- `build/diagnostico/spring.stdout.log` y `spring.stderr.log`: arranque y ejecución de Spring.
- `build/diagnostico/node*.log`: arranque inicial y restauración de Node.

La información esencial, resultados y pasos de reproducción están en este documento; estos archivos locales no se presuponen disponibles al clonar el repositorio o ejecutar `clean`.

## 13. Registro de comprobaciones HTTP

La tabla siguiente registra las 84 comprobaciones del arnés de diagnóstico. La columna esperado incluye el código HTTP que corresponde al caso, incluidos errores deliberados. Los seis fallos están asociados a D01 o D02; no se corrigieron durante esta etapa.


| N.º | Comprobación | Esperado | Observado | Resultado |
|---|---|---|---|---|
| 1 | E01 crear cliente principal | 201 | 201 | Correcto |
| 2 | E01 crear cliente eliminable | 201 | 201 | Correcto |
| 3 | E02 crear mascota principal | 201 | 201 | Correcto |
| 4 | E02 crear mascota secundaria | 201 | 201 | Correcto |
| 5 | E02 crear mascota eliminable | 201 | 201 | Correcto |
| 6 | Aux crear veterinario uno | 201 | 201 | Correcto |
| 7 | Aux crear veterinario dos | 201 | 201 | Correcto |
| 8 | Aux consultar cliente | 200 | 200 | Correcto |
| 9 | Aux actualizar cliente | 200 | 200 | Correcto |
| 10 | E03 listar mascotas | 200 | 200 | Correcto |
| 11 | Aux consultar mascota | 200 | 200 | Correcto |
| 12 | Aux actualizar mascota | 200 | 200 | Correcto |
| 13 | Aux listar veterinarios | 200 | 200 | Correcto |
| 14 | Datos invalidos cliente | 400 | 400 | Correcto |
| 15 | Email duplicado cliente | 409 | 409 | Correcto |
| 16 | Cliente inexistente | 404 | 404 | Correcto |
| 17 | Mascotas de cliente inexistente | 404 | 404 | Correcto |
| 18 | Mascota inexistente | 404 | 404 | Correcto |
| 19 | Reserva inexistente | 404 | 404 | Correcto |
| 20 | Mascota con peso invalido | 400 | 400 | Correcto |
| 21 | Mascota nacimiento futuro | 400 | 400 | Correcto |
| 22 | Crear mascota cliente inexistente | 404 | 404 | Correcto |
| 23 | JSON invalido Spring | 400 | 400 | Correcto |
| 24 | Veterinario invalido | 400 | 400 | Correcto |
| 25 | Veterinario duplicado | 409 | 409 | Correcto |
| 26 | E04 crear reserva principal | 201 | 201 | Correcto |
| 27 | Aux consultar reserva | 200 | 200 | Correcto |
| 28 | Reserva duplicada veterinario | 409 | 409 | Correcto |
| 29 | Reserva duplicada mascota | 409 | 409 | Correcto |
| 30 | Reserva fecha pasada | 400 | 400 | Correcto |
| 31 | Reserva mascota inexistente | 404 | 404 | Correcto |
| 32 | Reserva veterinario inexistente | 404 | 404 | Correcto |
| 33 | E05 modificar reserva | 200 | 200 | Correcto |
| 34 | E04 crear reserva cancelable | 201 | 201 | Correcto |
| 35 | Modificar a horario ocupado | 409 | 409 | Correcto |
| 36 | Atencion de reserva pendiente | 409 | 409 | Correcto |
| 37 | Transicion pendiente a atendida | 409 | 409 | Correcto |
| 38 | Estado invalido | 400 | 400 | Correcto |
| 39 | E06 confirmar y solicitar notificacion | 200 | 200 | Correcto |
| 40 | Confirmacion repetida sin nueva notificacion | 200 | 200 | Correcto |
| 41 | Atencion invalida | 400 | 400 | Correcto |
| 42 | E07 registrar atencion | 201 | 201 | Correcto |
| 43 | Reserva atendida con historial individual | 200 | 200 | Correcto |
| 44 | Atencion duplicada | 409 | 409 | Correcto |
| 45 | Modificar reserva atendida | 409 | 409 | Correcto |
| 46 | Cancelar reserva atendida | 409 | 409 | Correcto |
| 47 | Cancelar pendiente y notificar | 200 | 200 | Correcto |
| 48 | Modificar reserva cancelada | 409 | 409 | Correcto |
| 49 | Reactivar reserva cancelada | 409 | 409 | Correcto |
| 50 | E04 crear reserva horario-liberado | 201 | 201 | Correcto |
| 51 | Confirmar segunda reserva | 200 | 200 | Correcto |
| 52 | Cancelar confirmada y notificar | 200 | 200 | Correcto |
| 53 | E08 medicamento encontrado | 200 | 200 | Correcto |
| 54 | Medicamento busqueda parcial mayusculas | 200 | 200 | Correcto |
| 55 | Medicamento parametro vacio | 400 | 400 | Correcto |
| 56 | Medicamento sin resultados | 404 | 404 | Correcto |
| 57 | E09 ubicacion | 200 | 200 | Correcto |
| 58 | E10 notificacion aceptada simulada | 202 | 202 | Correcto |
| 59 | Notificacion sin campos | 400 | 400 | Correcto |
| 60 | JSON invalido Node | 400 | 400 | Correcto |
| 61 | DEFECTO notificacion campos null | 400 | 202 | Defecto reproducido |
| 62 | DEFECTO notificacion tipos y canal invalidos | 400 | 202 | Defecto reproducido |
| 63 | Ruta Node inexistente | 404 | 404 | Correcto |
| 64 | Preflight CORS Spring permitido | 200/204 | 200 | Correcto |
| 65 | Preflight CORS Node | 200/204 | 204 | Correcto |
| 66 | Aux eliminar mascota logicamente | 204 | 204 | Correcto |
| 67 | Mascota eliminada no consultable | 404 | 404 | Correcto |
| 68 | Listado excluye mascota eliminada | 200 | 200 | Correcto |
| 69 | Reserva mascota eliminada rechazada | 404 | 404 | Correcto |
| 70 | Aux eliminar cliente logicamente | 204 | 204 | Correcto |
| 71 | Cliente eliminado no consultable | 404 | 404 | Correcto |
| 72 | Mascota de cliente eliminado desactivada | 404 | 404 | Correcto |
| 73 | Reserva de mascota de cliente eliminado rechazada | 404 | 404 | Correcto |
| 74 | E04 crear reserva sin-node | 201 | 201 | Correcto |
| 75 | Node caido confirmar reserva | 200 | 200 | Correcto |
| 76 | Node caido cambio conservado | 200 | 200 | Correcto |
| 77 | Node caido repetir confirmacion no reintenta | 200 | 200 | Correcto |
| 78 | Node recuperado cancelar y notificar | 200 | 200 | Correcto |
| 79 | Eliminar cliente con historial | 204 | 204 | Correcto |
| 80 | Atencion conservada tras baja logica | 200 | 200 | Correcto |
| 81 | DEFECTO metodo no permitido clientes | 405 | 500 | Defecto reproducido |
| 82 | DEFECTO metodo no permitido reservas | 405 | 500 | Defecto reproducido |
| 83 | DEFECTO ruta historial no implementada | 404 | 500 | Defecto reproducido |
| 84 | DEFECTO raiz sin frontend | 404 | 500 | Defecto reproducido |
