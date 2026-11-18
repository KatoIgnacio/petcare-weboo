# Frontend de PetCare, etapa 5

La interfaz de gestión veterinaria abre en [PetCare local](http://localhost:8082/). Spring Boot sirve el HTML, el CSS y los módulos JavaScript incluidos en el JAR. El navegador usa rutas relativas de la API y Spring guarda las operaciones en PostgreSQL.

## Punto de partida

| Repositorio | Rama utilizada | Función en esta etapa |
|---|---|---|
| petcare-weboo | chore/diagnostico-inicial | Frontend, pruebas Spring y documentación |
| petcare-external-api | develop | Servicio Node usado para verificar el aviso por SMTP local |

El trabajo comenzó con los cambios locales de las etapas 3 y 4 presentes en ambas ramas. Los informes anteriores siguen siendo el registro de esas verificaciones. [Backend de la etapa 3](backend-etapa3.md), [APIs externas de la etapa 4](apis-externas-etapa4.md)

## Pantallas y contratos

| Pantalla | Operación | Método y ruta |
|---|---|---|
| Clientes y mascotas | Listar y crear clientes | GET y POST `/api/clientes` |
| Ficha de cliente | Consultar, editar y dar de baja | GET, PUT y DELETE `/api/clientes/{id}` |
| Mascotas del cliente | Listar las activas y crear | GET y POST `/api/clientes/{clienteId}/mascotas` |
| Ficha de mascota | Consultar, editar y dar de baja | GET, PUT y DELETE `/api/mascotas/{id}` |
| Veterinarios | Listar activos y registrar | GET y POST `/api/veterinarios` |
| Agenda | Listar reservas y filtrar por fecha o veterinario | GET `/api/reservas` con `fecha` y `veterinarioId` opcionales |
| Nueva reserva | Elegir cliente, mascota activa, veterinario y hora | POST `/api/reservas` |
| Detalle de reserva | Consultar y reprogramar | GET y PUT `/api/reservas/{id}` |
| Confirmación y cancelación | Cambiar el estado | PATCH `/api/reservas/{id}/estado` |
| Atención médica | Guardar diagnóstico, tratamiento, medicamentos y observaciones | POST `/api/reservas/{id}/atencion` |
| Historial clínico | Consultar las atenciones de una mascota | GET `/api/mascotas/{id}/historial` |

La navegación utiliza enlaces con fragmentos. La ficha de cliente se puede abrir y recargar en su propia dirección. El historial se abre desde una mascota activa o desde una reserva que la referencia, incluso si la mascota o su cliente fueron dados de baja. Las confirmaciones explican el efecto de la baja lógica antes de enviar la solicitud.

## Decisiones de implementación

- La agenda es la vista inicial y sus conteos reflejan las reservas que devuelven los filtros aplicados. La interfaz presenta el listado ordenado por Spring.
- Los formularios utilizan etiquetas explícitas, controles nativos y un diálogo que se puede cerrar con Escape. Durante una escritura se bloquea otro envío desde el mismo formulario. Si la operación devuelve un error, la interfaz conserva los valores y presenta `mensaje` y `detalles` de ApiError.
- Las respuestas 204 se procesan como operaciones exitosas sin cuerpo. Las consultas tienen mensajes para carga, resultados vacíos y errores de conexión. Una respuesta de escritura incierta pide revisar el estado antes de repetir la operación.
- La reserva envía `fechaHora` como hora local ISO. Al reprogramar, un cambio exclusivo del veterinario conserva la precisión de la hora guardada. Las fechas de agenda o nacimiento usan `YYYY-MM-DD`.
- Una reserva confirmada permite registrar su atención. La interfaz recarga el detalle para reflejar `ATENDIDA`. Los medicamentos se cargan uno por línea y se envían como arreglo.
- E06 informa el cambio de reserva y, cuando corresponde, que el aviso está pendiente. Al abrir o actualizar el detalle se lee el último resultado persistido. `ACEPTADA` se muestra como **Aceptado por el servidor SMTP**. Los resultados del aviso se presentan independientemente del estado de la reserva.
- La primera foto de cabecera se generó con ImageGen integrado y se conserva como parte de la evidencia inicial de la etapa. [Imagen de la primera versión](../src/main/resources/static/img/agenda-mascotas.webp)

<details>
<summary>Prompt de la imagen de cabecera</summary>

> Photorealistic editorial hero banner asset for PetCare, a veterinary scheduling application. Landscape 3:2. Warm fawn retriever puppy with floppy ears next to a small grey tabby cat, both at rest looking toward camera, shoulders and paws visible, framed on the right side and lower half, both full faces unobstructed. Seamless light pastel lavender background (#ece4fa) with soft even studio light, subtle plush textured surface, premium veterinary brand photography, natural fur and eyes, believable anatomy. Leave left half empty in seamless matching lavender for interface text overlay outside the image. No rendered text, no marks, no watermark, no additional objects.

</details>

## Verificación

Las pruebas se realizaron entre el 17 y el 18 de noviembre de 2026 en un computador, con Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 17.11, Node local y Chrome automatizado mediante Playwright. Se creó la base separada `petcare_etapa5_20261117`. Los datos generados se identificaron como Etapa5 y los correos se dirigieron a Mailpit.

### Pruebas Spring

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
.\gradlew.bat check bootJar
```

El comando aprobó 45 pruebas y generó `petcare-0.0.1-SNAPSHOT.jar`. El cambio previsto en `GET /` se comprobó con una prueba de HTML y recursos JavaScript y CSS. La ruta de API inexistente sigue cubierta por la comprobación de 404 con ApiError.

### Recorrido en el navegador

| Comprobación | Resultado observado |
|---|---|
| Raíz y recursos | La agenda abre desde el JAR y carga JavaScript, CSS e imagen local |
| Campos inválidos | El navegador detecta un correo inválido. Spring devuelve 400 para el nombre vacío y la interfaz conserva los demás campos |
| Clientes y mascotas | Alta, consulta y edición desde la interfaz, con datos recuperados tras recargar |
| Historial vacío | La interfaz avisa que la mascota todavía no tiene atenciones |
| Veterinarios | Alta visible en el listado y disponible en el selector de reservas |
| Nueva reserva y duplicado | Creación 201 con la hora local elegida. El duplicado devuelve 409 y el formulario queda listo para corregir |
| Filtros y reprogramación | Consultas con fecha, veterinario y ambos parámetros. Actualización 200 y nueva hora visible en la agenda |
| Confirmación y atención | Reserva confirmada con aviso inicialmente pendiente. La atención se registró con 201 y la reserva pasó a Atendida |
| Cancelación y aviso | Cancelación 200 conservada al recargar. La actualización manual del detalle mostró la aceptación de SMTP |
| Bajas e historial | Dos bajas con 204. La baja de cliente desactivó a su mascota asociada. La agenda mantuvo acceso a la atención de una mascota dada de baja |
| Fallo de conexión y consulta vacía | El listado mostró estado vacío. Se interrumpió una solicitud de agenda en Chrome y se recuperó con Volver a intentar |
| Escritorio, móvil y teclado | Revisión a 1440 × 940 y 390 × 844. El ancho móvil quedó dentro de la pantalla. El teclado permitió saltar al contenido, abrir el formulario, recorrer sus controles y volver al botón al cerrar |

La interrupción de red se aplicó a una solicitud del navegador durante la prueba. Las caídas reales, los reintentos y la coordinación de notificaciones tienen evidencia en el informe de la etapa 4.

## Recorrido reproducible para la demostración

1. Abre PetCare y registra un cliente de prueba con un correo único terminado en `@example.invalid`. Edita el teléfono antes de agregar una mascota. Crea una segunda mascota para poder mostrar sus fichas y el historial vacío.
2. Registra un veterinario. En Agenda, crea una reserva con un horario futuro. Vuelve a intentar el mismo horario con la misma mascota y el mismo veterinario para mostrar el conflicto.
3. Filtra por el día y el veterinario. Reprograma la primera reserva, confirma el nuevo horario y registra una atención.
4. Abre el historial desde el detalle de la reserva. Da de baja esa mascota desde su ficha y vuelve a abrir el historial desde la agenda.
5. Crea una reserva para la otra mascota y cancélala. Recarga la página y vuelve a abrir el detalle.
6. Con Node y Mailpit activos, usa **Actualizar detalle** y revisa en Mailpit el aviso de confirmación o cancelación. Para el video, muestra el registro correspondiente en PostgreSQL junto a su resultado en la interfaz.

Los pasos de instalación, creación de una base de prueba e inicio de los servicios están en el README. [Inicio local](../README.md)

## Evidencia y servicios

En la última ejecución se comprobaron directamente en PostgreSQL la reserva 5, reprogramada al 21 de noviembre a las 11:45:35 y guardada como ATENDIDA con atención 3, y la reserva 6, guardada como CANCELADA a las 13:15. Ambas tenían un aviso ACEPTADA. También se comprobó que la mascota 10 quedó inactiva y el cliente 6 siguió activo.

Mailpit capturó **Reserva confirmada** y **Reserva cancelada** para `etapa5.camila.mwljjrbc@example.invalid`. Se inspeccionaron el remitente, el destinatario, el asunto y el texto que contiene el horario correspondiente. [Mailpit local](http://localhost:8025/)

La evidencia local está en `build/etapa5`. Incluye `verificacion-navegador.json` con los recorridos y respuestas HTTP, `mailpit-navegador.json` con ambos mensajes, las capturas `agenda-escritorio.png`, `agenda-movil.png` y `formulario-movil.png`, y los registros de inicio de Spring y Node. Gradle genera los detalles de sus pruebas en `build/reports/tests/test`.

| Servicio | Estado al entregar |
|---|---|
| Spring Boot, puerto 8082 | Iniciado para esta etapa y activo sobre la base de prueba |
| API Node, puerto 3001 | Iniciada para esta etapa y activa con SMTP de captura local |
| PostgreSQL, puerto 5433 | Activo desde antes de esta etapa, con el volumen existente |
| Mailpit, puertos 1025 y 8025 | Activo desde antes de esta etapa |

## Pendientes

- Etapa 6. Consumir medicamentos y ubicación desde el navegador y completar el seguimiento visual de los avisos.
- Etapa 7. Actualizar Postman y preparar la verificación integral.
- Etapa 8. Comprobar la comunicación entre dos computadores.
- Etapa 9. Grabar y preparar la entrega con las dos pantallas y voces según la guía.

Los cambios de esta etapa quedan disponibles para revisión local.

## Rediseño de la interfaz

La interfaz usa verde bosque, salvia y marfil, con títulos serif y una composición más despejada. El botón circular flotante alterna entre claro y oscuro. Sigue la preferencia del sistema al abrir PetCare por primera vez y guarda las selecciones explícitas en el navegador. Las transiciones se reducen cuando el sistema solicita movimiento reducido.

La agenda, los listados y los diálogos muestran esqueletos mientras esperan sus consultas. Al guardar, la interfaz indica la operación en curso y bloquea otro envío del mismo formulario. Cuando se recibe la respuesta, confirma el resultado y muestra la actualización del listado si corresponde.

Se verificaron las preferencias de tema, los estados de carga con solicitudes ralentizadas, el guardado con una respuesta real, el teclado y la presentación a 390 px. Se repitieron `check bootJar` con Java 21 y los once recorridos de la etapa 5 en Chrome. Las capturas del rediseño y los resultados se guardaron en `build/etapa5`, con el prefijo `rediseno-` y en `verificacion-rediseno.json`.

La nueva fotografía se generó con ImageGen integrado y se sirve en WebP desde el mismo servidor. [Foto de cabecera](../src/main/resources/static/img/agenda-mascotas-verde.webp)

<details>
<summary>Prompt de la nueva foto</summary>

> Create a photorealistic editorial photograph for the right-hand half of an elegant veterinary clinic scheduling dashboard. Landscape width-to-height ratio 3:2. On the far right sits a beautiful honey-apricot retriever puppy and in front of it on the center-right is a grey tabby kitten; both look gently toward camera, natural anatomy, whole heads and front paws visible within the central vertical 70 percent for a short banner crop. Keep the leftmost 32 percent unoccupied. Seamless warm grey-green sage (#dfe9df) studio wall and surface, subtle natural window light, restrained premium editorial photography, clear texture and quiet personality. Sufficient headroom and space at bottom. Soft open shadows, no props, no people, no text, no logos, no watermarks. Thoroughly cohesive with mint, dark pine green and warm off-white interface palette.

</details>
