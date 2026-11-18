# APIs externas y notificaciones de PetCare

## Ejecución de la etapa 4

Verificación realizada el 17 de noviembre de 2026, con hora local America/Santiago. Se utilizaron las especificaciones y los informes de las etapas 1, 2 y 3. Los informes anteriores conservan sus resultados históricos.

| Repositorio | Rama utilizada | HEAD al comenzar |
|---|---|---|
| petcare-weboo | chore/diagnostico-inicial | 69712b6 |
| petcare-external-api | develop | f75ddc1 |

El repositorio principal ya tenía cambios locales de la etapa 3. Se conservaron esos cambios y se continuó en su rama de desarrollo. El repositorio externo comenzó limpio. Los cambios de esta etapa quedan preparados para revisión local y pendientes de commit y publicación.

## Entorno comprobado

| Componente | Versión o dirección |
|---|---|
| JDK utilizado por Gradle y Spring | Java 21.0.12.1 |
| Gradle Wrapper | 9.7.1 |
| Spring Boot | 4.1.1 |
| Node / npm | 24.15.0 / 11.12.1 |
| Docker | 29.9.2 |
| PostgreSQL | 17.11, localhost:5433 |
| Spring Boot | localhost:8082 |
| API Node | localhost:3001, desde el repositorio independiente |
| SMTP local | Mailpit v1.27.4, localhost:1025 |
| Mensajes SMTP | http://localhost:8025 |
| Base para integración | petcare_diagnostico_20261117 |
| Volumen PostgreSQL conservado | petcare-weboo_petcare_data |

Docker y PostgreSQL estaban activos al comenzar. Los puertos 8082, 3001, 1025 y 8025 estaban disponibles. Se inició Mailpit con un volumen propio `petcare-external-api_mailpit_data`. Se comprobó la disponibilidad antes de iniciar procesos y se identificaron los procesos propios antes de detenerlos.

Se ejecutaron satisfactoriamente las 26 pruebas existentes antes de implementar. Gradle requirió acceso a su caché del usuario mediante el mecanismo de aprobación del entorno. Las pruebas Java usan H2 con creación de esquema de pruebas y el trabajador periódico desactivado. La integración posterior utilizó PostgreSQL y registros nuevos identificados con Etapa4.

## Archivos de esta etapa

En petcare-external-api

- `server.js` implementa validación, errores, envío SMTP e idempotencia HTTP.
- `config.js` valida configuración HTTP, clínica y SMTP.
- `receipts.js` persiste recibos y coordina una instancia por archivo.
- `package.json` y `package-lock.json` incorporan Nodemailer 10.6.0 y smtp-server 3.21.6 como dependencia de pruebas.
- `test/api.test.js` cubre contratos, SMTP y persistencia.
- `compose.yaml` inicia Mailpit con almacenamiento propio.
- `.env.example`, `.gitignore` y `README.md` documentan configuración y conservan datos locales fuera de Git.

En petcare-weboo

- `entity/EstadoNotificacion.java` y `entity/NotificacionPendiente.java` modelan los avisos.
- `repository/NotificacionRepository.java` consulta pendientes y reclama eventos con bloqueo breve.
- `service/NotificacionService.java` registra y recupera resultados.
- `service/NotificacionDispatcher.java` aplica concesión temporal, token, límites y reintentos.
- `service/NotificacionScheduler.java` ejecuta el trabajador periódico.
- `config/NotificacionConfig.java` y `config/NotificacionProperties.java` configuran reloj y política.
- `service/ReservaService.java` registra eventos transaccionales y permite consultar el resultado guardado.
- `service/NotificacionClient.java` envía la clave estable e interpreta aceptación, fallos e incertidumbre.
- `config/ExternalApiConfig.java` hace configurables los tiempos de espera HTTP.
- `application.properties` y `schema.sql` incorporan configuración y tabla compatible con datos existentes.
- `NotificacionesTests.java` y `NotificacionClientTests.java` añaden comprobaciones. Los fixtures de las dos suites anteriores limpian también los avisos del conjunto de prueba.
- El README y este informe documentan la ejecución y los resultados.

Las rutas Java anteriores pertenecen a `src/main/java/cl/bello/petcare`. Las pruebas pertenecen a `src/test/java/cl/bello/petcare`. La configuración de pruebas está en `src/test/resources/application.properties`.

## Estrategia implementada y corrección D05

Se eligió una tabla de avisos pendientes en PostgreSQL y un trabajador periódico en Spring. El registro del evento participa en la transacción que confirma o cancela la reserva. El trabajador consulta únicamente datos confirmados. Reclama un aviso en una transacción breve y realiza la llamada HTTP después de liberarla. Luego guarda el resultado mediante otra transacción breve.

La tabla conserva clave UUID, reserva, transición, destinatario y contenido capturados al crear el evento, estado, intentos, siguiente ejecución, concesión temporal, token de intento, identificador externo, último error y tiempos UTC. La restricción única sobre reserva y transición impide crear dos avisos para el mismo cambio. El SQL agrega la tabla con CREATE TABLE IF NOT EXISTS. Hibernate valida su correspondencia al iniciar PostgreSQL con los datos anteriores presentes.

Los eventos usan PENDIENTE, EN_PROCESO, PENDIENTE_REINTENTO, ACEPTADA, FALLIDA e INCIERTA. Las reservas conservan sus estados y la interpretación local de fechaHora. La clave estable viaja a Node mediante Idempotency-Key. Confirmar y cancelar generan claves diferentes. Repetir el estado actual devuelve NO_APLICA y conserva el aviso existente.

La política predeterminada permite cinco intentos totales, con esperas exponenciales de 30, 60, 120 y 240 segundos y un tope configurable de cinco minutos. El ciclo predeterminado es de cinco segundos. La concesión de un intento dura 30 segundos. La aplicación recupera pendientes y concesiones vencidas al reiniciar. Un último intento interrumpido queda INCIERTA. Los fallos temporales conocidos que agotan los intentos quedan FALLIDA.

Para acelerar las pruebas integradas se usaron tres intentos, espera inicial de tres segundos, tope de seis segundos, ciclo de 500 ms, concesión de diez segundos, HTTP de tres segundos y SMTP de un segundo. Estos valores se asignaron al entorno de los procesos de prueba. Los valores predeterminados permanecen documentados en configuración y README.

Los bloqueos de reserva terminan al confirmar la operación de negocio. Las esperas de red y de reintento ocurren fuera de esos bloqueos. Los tokens impiden que un trabajador anterior sobrescriba un intento reclamado posteriormente.

## Contratos finales

### E08 Medicamentos

GET /api/medicamentos?nombre=... conserva un arreglo de objetos con nombre, principioActivo, presentacion, advertencias y fuente. Recorta espacios, ignora mayúsculas y permite coincidencias parciales. El nombre es obligatorio y admite hasta 120 caracteres. Una consulta vacía o inválida devuelve 400. La ausencia de coincidencias devuelve 404.

El catálogo conserva sus tres registros demostrativos y la fuente Catálogo demostrativo PetCare. Las pruebas usan ese catálogo existente.

### E09 Ubicación

GET /api/ubicacion-clinica conserva nombre, direccion, latitud, longitud y urlMapa. Las coordenadas son numéricas, finitas y dentro de sus rangos. El enlace OpenStreetMap se genera desde las coordenadas. CLINIC_NAME, CLINIC_ADDRESS, CLINIC_LATITUDE y CLINIC_LONGITUDE permiten sustituir los datos demostrativos. La configuración inválida produce 503 con UBICACION_INVALIDA.

### E10 y corrección D01

POST /api/notificaciones conserva destinatario, canal, asunto, mensaje y reservaId. Valida objeto JSON, campos obligatorios, nulos, tipos, texto, correo único, canal EMAIL y reservaId entero seguro entre 1 y 9007199254740991. Los límites de cadenas son 254, 10, 200 y 10000 caracteres respectivamente. Asunto rechaza CR/LF. El cuerpo admite por defecto 65536 bytes.

Se reprodujeron las entradas D01 con todos los valores nulos y con destinatario numérico, canal incompatible, asunto arreglo, mensaje objeto y reservaId negativo. Ahora producen 400. La suite cubre además los campos individuales ausentes, nulos, vacíos y de tipos incorrectos.

| Respuesta | Condición |
|---|---|
| 202 con idNotificacion y estado ACEPTADA | SMTP acepta y Node guarda el resultado |
| 400 | Datos, JSON o clave inválidos |
| 413 | Cuerpo excesivo |
| 415 | Content-Type incompatible |
| 409 CONFLICTO_IDEMPOTENCIA | Clave existente con contenido diferente |
| 409 ENVIO_INCIERTO | Resultado que requiere revisión |
| 503 | Infraestructura, configuración SMTP, almacenamiento o rechazo SMTP |

Los errores conservan mensaje e incorporan codigo y, según el caso, campos, reintentable e idNotificacion. La cabecera opcional Idempotency-Key admite 1 a 100 caracteres alfanuméricos, guiones o guiones bajos. Las solicitudes originales de cinco campos siguen siendo válidas y generan una clave por petición.

### E06 y consulta del resultado

E06 conserva todos los campos existentes. Una transición nueva devuelve resultadoNotificacion con PENDIENTE y la clave UUID del evento. La respuesta ocurre después del commit. GET /api/reservas/{id} recupera el estado persistido del último aviso. Los resultados disponibles son PENDIENTE:UUID, EN_PROCESO:UUID, PENDIENTE_REINTENTO:UUID, ACEPTADA:UUID, FALLIDA:UUID e INCIERTA:UUID. ACEPTADA incluye el idNotificacion emitido por Node. Los otros estados incluyen la clave del evento.

Esta extensión sustituye la espera SMTP durante la transacción de reserva y permite consultar el resultado del trabajo asíncrono. La repetición del estado actual mantiene NO_APLICA. Las reservas anteriores sin eventos también producen NO_APLICA al consultar su aviso. La agenda, los historiales y ApiError conservaron los contratos comprobados en la etapa 3.

## SMTP, persistencia e incertidumbre

Nodemailer realiza un envío SMTP. Node guarda primero un recibo PROCESANDO con UUID y hash del contenido normalizado. Tras la aceptación SMTP guarda ACEPTADA. El archivo se escribe mediante un temporal sincronizado y reemplazo. Un bloqueo por PID coordina una instancia Node por archivo. Conservar ese archivo permite repetir la misma clave tras reiniciar sin otro envío cuando ya existe aceptación registrada.

ACEPTADA representa aceptación por el servidor SMTP y registro del resultado en Node. Los mensajes capturados en Mailpit aportan evidencia del destinatario, remitente, asunto y cuerpo. La entrega a una casilla externa y su lectura requieren comprobaciones adicionales con esa casilla.

Un rechazo SMTP permanente se registra como FALLIDA. Los fallos temporales conocidos permiten repetir la misma clave y mantener su identificador. Una pérdida de socket sin respuesta SMTP fiable, un timeout de socket, un recibo PROCESANDO encontrado al reiniciar o un fallo al guardar después de la aceptación quedan sujetos a revisión como INCIERTA. El Message-ID estable permite localizar el envío.

La prueba estricta del timeout detectó que Nodemailer puede informar la pérdida posterior a DATA con comando CONN. Se corrigió la clasificación usando también timeoutType y los errores de socket. La prueba conservó la exigencia de 409 ENVIO_INCIERTO y pasó tras la corrección.

Los casos inciertos y definitivos detienen el procesamiento automático y conservan evidencia para revisión. La persistencia de ambos repositorios y la reutilización de la clave sostienen la prevención de repeticiones comprobada. La pérdida del archivo de recibos o una clave nueva permiten generar otro correo. La incertidumbre SMTP requiere una decisión posterior basada en evidencia del servidor o proveedor.

## Resultados comprobados

| Verificación | Resultado |
|---|---|
| Pruebas Java iniciales | 26 aprobadas |
| Pruebas Java finales | 44 aprobadas, cero fallos y cero errores |
| Desglose final Java | 23 BackendEndpointsTests, 3 PetcareApplicationTests, 10 NotificacionesTests y 8 NotificacionClientTests |
| Gradle check y bootJar | BUILD SUCCESSFUL con Java 21 |
| Node | 48 pruebas aprobadas, cero fallos |
| Regresión HTTP PostgreSQL de etapa 3 | 95 de 95 comprobaciones aprobadas |
| Concurrencia de etapa 3 | Cuatro escenarios aprobados |
| Integración específica de etapa 4 | 14 de 14 escenarios aprobados con el JAR final |
| Correos inspeccionados en la última integración | Seis mensajes completos |
| Conservación de datos anteriores | Comparación satisfactoria de filas en las siete tablas |

Las suites Java comprueban registro de confirmación y cancelación, rollback, invisibilidad del evento antes del commit, ausencia de HTTP dentro de la transacción, persistencia, recuperación por un trabajador nuevo, espera exponencial, agotamiento, incertidumbre, repetición de transición y coordinación entre trabajadores.

Las pruebas Node utilizan SMTP real sobre un puerto efímero y dependencias controladas. Comprueban fallos temporales, rechazos permanentes, timeout real posterior a DATA, idempotencia concurrente, reinicio de recibos, fallo de disco antes y después del envío y validación D01.

Los cuatro escenarios concurrentes de regresión comprobaron el mismo veterinario y horario, la misma mascota y horario, reprogramación al mismo horario y atención única. Las respuestas fueron 201/409, 201/409, 200/409 y 201/409, con un único registro válido en cada caso. Se adaptó la comprobación del resultado inmediato de notificación a PENDIENTE conforme al contrato asíncrono. Las reglas de negocio y los códigos HTTP conservaron sus aserciones.

### Integración real de fallos y recuperación

Los 14 escenarios comprobaron registro de cliente, mascota, veterinario y reservas, confirmación con correo, cancelación de otra reserva con correo, repetición sin aviso adicional, caída Node con reserva confirmada en PostgreSQL, reinicio real de Spring con pendiente, restauración Node, caída y restauración SMTP, cuatro solicitudes E10 simultáneas con un único mensaje, reinicio Node con recibo conservado, conflicto de clave, tres confirmaciones simultáneas con un evento, agotamiento real de tres intentos, consultas de clientes y agenda, atención e historial y conservación de datos anteriores.

La caída de Node mantuvo CONFIRMADA y PENDIENTE_REINTENTO. Tras reiniciar Spring con Node restaurado, el segundo intento quedó ACEPTADA. La caída SMTP produjo 503 reintentable y conservó la reserva. Al restaurar Mailpit se capturó el segundo intento. El agotamiento dejó la reserva CANCELADA y su aviso FALLIDA con tres intentos. Restaurar SMTP conservó ese fallo definitivo.

El primer arnés de integración quedó esperando un descriptor heredado al iniciar un proceso auxiliar. Se corrigió su control de stdio y se repitió la ejecución completa. Los datos de ese intento se conservaron identificados como Etapa4 y su aviso pendiente se recuperó antes de continuar.

## Evidencias locales

La última integración utilizó el destinatario `etapa4.1794951847834@example.invalid`, el remitente `avisos@petcare.example.invalid` y la mascota Etapa4 Milo. La confirmación incluyó la fecha local de la reserva y el texto quedó confirmada. La cancelación de otra reserva incluyó su fecha y quedó cancelada. Se inspeccionaron además correos de recuperación Node, recuperación SMTP, idempotencia y confirmación concurrente.

| Mensaje capturado | Identificador Mailpit |
|---|---|
| Reserva confirmada | bjWxe5t3N2gCRnEhwmXvg3 |
| Reserva cancelada | MG4yTzK3LNGPqYzS43Mv3X |
| Reserva confirmada | an3iJFkYgsVQLobn4Ft3Qm |
| Reserva confirmada | XAKNc6HjNStyGMeQgNpkue |
| Etapa4 idempotencia | CujoJWh6D9TfRAmR6wRe8V |
| Reserva confirmada | JpUufKLMQjxWF5n4t5sukQ |

La comparación de filas de la última ejecución conservó los registros anteriores de clientes, mascotas, veterinarios, reservas, atenciones, medicamentos de atención y avisos. Los datos Etapa4 creados durante las verificaciones permanecen en la base separada y los mensajes permanecen en Mailpit.

Los artefactos locales bajo build/etapa4 están ignorados por Git

- `integration-results.json` conserva los 14 escenarios, mensajes completos y recuentos de conservación.
- `integration-database-before.json` conserva las filas anteriores a la última ejecución.
- `regresion/http-results.json`, `regresion/concurrency-results.json`, `regresion/database-preservation.json` y `regresion/state.json` conservan las 95 verificaciones y los cuatro escenarios concurrentes.
- `services.json`, `services-end.json` y los archivos de registro identifican los procesos y el estado de cierre.
- Los arneses `verify.cjs` y `services.ps1` permiten revisar la ejecución realizada.

Los informes Java reproducibles se generan en build/reports/tests/test y build/test-results/test. Los comandos de instalación, pruebas, carga del entorno e inicio están en los README de ambos repositorios. SMTP se configura mediante variables del proceso o carga explícita con `node --env-file=.env server.js`. Las credenciales de un proveedor se mantienen en el entorno o en el archivo local ignorado.

## Limitaciones y verificaciones pendientes

- Entrega a casilla externa pendiente de configuración SMTP del proveedor y un destinatario autorizado. Se solicitó esa información durante el trabajo y se completó la captura SMTP local.
- Los casos INCIERTA y FALLIDA requieren revisión de la persistencia y la evidencia del servidor antes de decidir otro envío. Su gestión visual corresponde a las etapas del frontend.
- La persistencia de recibos está dimensionada para una instancia Node por archivo. La ejecución entre dos equipos requiere configurar direcciones y comprobar conectividad en esa red.
- La ubicación y el catálogo siguen identificados como demostrativos. Los datos reales de la clínica se configuran cuando sean proporcionados.
- El frontend, su integración visual, la revisión integral de Postman y la demostración entre equipos siguen sus etapas previstas.

Las verificaciones obligatorias locales de la etapa están satisfechas. La comprobación de una casilla externa conserva el pendiente indicado.

## Servicios al finalizar

Verificado a las 18:47:03, hora de Santiago.

| Servicio | Propiedad e inicio | Estado de cierre |
|---|---|---|
| Docker Desktop / motor | Ya activo al comenzar | Activo |
| PostgreSQL petcare-postgres | Ya activo, volumen conservado | Healthy, puerto 5433 |
| Mailpit petcare-mailpit | Iniciado por esta etapa, volumen propio | Healthy, SMTP 1025 e interfaz 8025 |
| Spring Boot | Iniciado y reiniciado por esta etapa | Detenido, puerto 8082 libre |
| API Node | Iniciada y reiniciada por esta etapa | Detenida, puerto 3001 libre |

Mailpit conserva 29 mensajes de los conjuntos Etapa4 y sus regresiones. Se conservaron las bases y los volúmenes de PostgreSQL.

Para revisar los mensajes, abrir [Mailpit local](http://localhost:8025) y localizar el destinatario de la última integración indicado arriba. Los README permiten iniciar nuevamente Spring y Node con la configuración de desarrollo.

Las opciones de envío, las pruebas SMTP y el despliegue del capturador se contrastaron con documentación oficial. [Nodemailer SMTP](https://nodemailer.com/smtp), [SMTP Server](https://nodemailer.com/extras/smtp-server), [Mailpit Docker](https://mailpit.axllent.org/docs/install/docker/)
