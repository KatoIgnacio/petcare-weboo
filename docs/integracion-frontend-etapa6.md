# Integración del frontend de PetCare con la API externa

Desde PetCare se pueden buscar medicamentos, consultar la ubicación de la clínica y seguir el aviso de una reserva. El navegador consulta directamente los recursos de Node y obtiene el estado de los avisos de Spring. [Abrir PetCare](http://127.0.0.1:8082/)

## Punto de partida

La etapa se ejecutó el 18 de noviembre de 2026 en un solo computador. El repositorio principal continuó en `chore/diagnostico-inicial` y el externo en `develop`, con sus cambios locales de etapas anteriores. La implementación de esta etapa se realizó en `petcare-weboo`. La API Node de `petcare-external-api` se utilizó como servicio independiente para las pruebas.

Las nuevas pantallas reutilizan el diseño, los temas claro y oscuro, los esqueletos y los componentes de respuesta de la interfaz de la etapa 5. [Informe de la etapa 5](frontend-etapa5.md)

## Configuración y endpoints

| Quién consulta | Destino | Uso |
|---|---|---|
| Navegador | Spring `GET /api/configuracion` | Carga la única propiedad pública `externalApiUrl`, tomada de `EXTERNAL_API_URL` |
| Navegador | Node `GET /api/medicamentos?nombre=...` | E08, consulta directa a Node con el término codificado |
| Navegador | Node `GET /api/ubicacion-clinica` | E09, consulta directa a Node y enlace al mapa |
| Navegador | Spring `PATCH /api/reservas/{id}/estado` y `GET /api/reservas/{id}` | Guarda la transición y obtiene el último aviso persistido |
| Spring | Node `POST /api/notificaciones` | E10, solicita el envío del aviso después de guardar la transición |

Spring sirve la configuración con `Cache-Control: no-store`. Acepta una URL HTTP o HTTPS con host y sin credenciales, consulta ni fragmento. Una dirección inválida genera un 503 con `ApiError`. Las credenciales SMTP quedan en las variables de entorno del proceso Node.

En local se utilizó `EXTERNAL_API_URL=http://127.0.0.1:3001` en la instancia de trabajo. Para probar la configuración, la instancia de verificación en 8086 se inició primero con `http://127.0.0.1:3002` y luego con `http://127.0.0.1:3001`. Chrome registró respuestas 200 directamente desde cada puerto para E08 y E09, con `Access-Control-Allow-Origin: *` en Node.

Para la demostración en red, inicia Node en el segundo computador con `HOST=0.0.0.0` o una interfaz de red alcanzable. Configura en la terminal de Spring `EXTERNAL_API_URL` con la IP del segundo equipo antes de iniciarlo. Desde el primero, el navegador y Spring deben poder conectar con esa dirección y puerto. [Pasos de inicio local](../README.md)

## Medicamentos y clínica

- **Medicamentos** admite términos de 1 a 120 caracteres después de recortar los espacios exteriores. La interfaz muestra nombre, principio activo, presentación, advertencias y fuente. `404 SIN_RESULTADOS` muestra un resultado vacío. Los errores de red, tiempo de espera y respuestas con formato inesperado mantienen una acción de reintento. Al iniciar otra búsqueda o cambiar de pantalla se cancela la consulta anterior.
- **Clínica** valida el nombre, la dirección, los rangos de latitud y longitud y el enlace HTTP o HTTPS antes de mostrar el mapa. La interfaz identifica la ubicación de ejemplo por su dirección y sus coordenadas documentadas. Para usar datos reales, configura en Node `CLINIC_NAME`, `CLINIC_ADDRESS`, `CLINIC_LATITUDE` y `CLINIC_LONGITUDE`.

## Avisos en el detalle de reserva

Al confirmar o cancelar, el detalle muestra el estado guardado de la reserva y un bloque independiente para el aviso. Un resultado inicial `PENDIENTE:<claveEvento>` se muestra de inmediato. Si la transición devuelve `NO_APLICA`, se recupera el estado vigente con `GET /api/reservas/{id}`.

| Estado | Presentación |
|---|---|
| NO_APLICA | Sin aviso registrado |
| PENDIENTE | Pendiente de envío |
| EN_PROCESO | En proceso |
| PENDIENTE_REINTENTO | Pendiente de reintento |
| ACEPTADA | Aceptado por el servidor SMTP |
| FALLIDA | Envío fallido |
| INCIERTA | Resultado incierto. Requiere revisión |

El detalle consulta cada 2,5 segundos mientras el aviso siga en curso, con un límite de un minuto. Realiza una consulta a la vez. Al cerrar o reemplazar el detalle se cancela su temporizador y, si está abierta, su petición. Los estados finales detienen el seguimiento. Si falla una consulta, la interfaz conserva el último estado visible y ofrece **Actualizar detalle**. El botón permite reiniciar el seguimiento manualmente.

El mensaje de confirmación de la interfaz informa el cambio de reserva. El bloque de notificación muestra el resultado vigente del aviso. El envío y sus reintentos se gestionan en Spring y Node, con Mailpit como destino SMTP de pruebas. [Contratos y persistencia de la etapa 4](apis-externas-etapa4.md)

## Verificación

Se usó PostgreSQL 17.11 con la base separada `petcare_etapa6_20261118`, Java 21, Chrome y Mailpit local. Los registros escritos se identificaron como Etapa6, o como Etapa5 en los recorridos de regresión. Las instancias de prueba se ejecutaron en los puertos 8086 y 3002. Las transiciones se siguieron con un intervalo de trabajo de Spring reducido en esa instancia de pruebas.

| Comprobación | Evidencia |
|---|---|
| Java y empaquetado | `.\gradlew.bat check bootJar` con 46 pruebas aprobadas. Incluye la propiedad pública de configuración y la cabecera de caché |
| E08 real | `AMOX` devuelve Amoxicilina y los cinco campos. La búsqueda vacía se valida en el navegador y un término sin registro devuelve 404 |
| E09 real | Respuesta 200 con los datos de ejemplo y el enlace válido. Una instancia Node iniciada con latitud 91 devolvió 503 `UBICACION_INVALIDA` |
| Configuración entre puertos | Chrome consumió E08 y E09 directamente de 3002 y de 3001 al cambiar `EXTERNAL_API_URL` y reiniciar la instancia Spring de pruebas |
| Caída real y recuperación de Node | Se detuvo la instancia de 3002. Ambas vistas externas informaron la desconexión. Se guardaron cliente, mascota, veterinario, reservas, confirmación y atención a través de Spring. El aviso pasó a pendiente de reintento y llegó a ACEPTADA al restaurar Node |
| Envíos reales a SMTP local | La confirmación y la cancelación generaron dos mensajes en Mailpit. El navegador consultó sus resultados en Spring |
| Regresiones | Se repitieron los 11 recorridos de la etapa 5, incluidos duplicado 409, fechas locales, reprogramación, historial tras la baja, estados persistidos y navegación con teclado |
| Presentación | Se revisó escritorio a 1440 px y móvil a 390 px en ambos temas. Se comprobó la búsqueda con teclado y que el botón circular de tema queda fuera del enlace al mapa |

Las simulaciones controladas en Chrome cubrieron una respuesta de búsqueda tardía, un catálogo incompleto, una URL de configuración inválida, la interrupción de una consulta de aviso, el cierre con Escape, el límite de un minuto, los resultados FALLIDA e INCIERTA y la consulta de un recibo vigente tras recibir `NO_APLICA` en E06. Estas respuestas de prueba se inyectaron en el navegador.

## Correos y estados persistidos

La reserva 5, del 21 de noviembre a las 09:30, quedó ATENDIDA con la atención 3. Su aviso terminó ACEPTADA después de tres intentos durante la caída y recuperación de Node. La reserva 6, del mismo día a las 11:00, quedó CANCELADA y su aviso terminó ACEPTADA en un intento.

Mailpit capturó **Reserva confirmada** (JXKjxkmoSbiyD5raKsbTvt) y **Reserva cancelada** (k28RgVmomfcic2A8H3EJr7) para `etapa6.mwlllt4x@example.invalid`. Se comprobaron remitente, destinatario, asunto y horario incluido en el cuerpo. [Mailpit local](http://127.0.0.1:8025/)

El directorio local `build/etapa6` contiene `navegador-evidencia.json` con los pasos, las URLs, los estados HTTP y el contenido de los mensajes, `postgresql-evidencia.txt` con la consulta independiente, capturas de las pantallas y `avisos-simulados.log` con las verificaciones adicionales. Los resultados Java se generan en `build/reports/tests/test`.

## Estado de los servicios y pendientes

Durante la revisión se usó Spring en 8082 con el JAR actualizado y la base `petcare_etapa5_20261117`, Node en 3001, PostgreSQL y Mailpit. Las instancias de prueba en 8086 y 3002 se detuvieron después de la verificación.

Al cerrar la jornada se detuvieron Spring, Node, los contenedores de PostgreSQL y Mailpit y Docker Desktop. Se conservaron los volúmenes, las bases de prueba y los mensajes capturados. [Prompt para retomar la etapa 7](prompt-etapa7.md)

- **Etapa 7** · Actualizar Postman y completar la verificación integral de la entrega.
- **Etapa 8** · Configurar la IP y comprobar el tráfico entre los dos computadores durante la demostración.
- **Correo externo** · La entrega a Gmail se podrá probar al configurar una cuenta y autorizar un destinatario. La evidencia de esta etapa corresponde al SMTP local.
