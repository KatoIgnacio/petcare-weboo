# Prompt para retomar PetCare · Etapa 7

## Encargo

Ejecuta la etapa 7 de PetCare. Actualiza y ejecuta las pruebas de Postman y comprueba el recorrido integrado desde el navegador. Entrega la colección, el entorno y un informe reproducible para preparar la demostración final.

## Repositorios y fuentes

- Principal `C:\Users\jaime\Desktop\JaimePalavecino\Universidad\Codigos\WebOO\petcare-weboo`.
- API externa `C:\Users\jaime\Desktop\JaimePalavecino\Universidad\Codigos\WebOO\petcare-external-api`.
- Lee los `AGENTS.md` aplicables, los README y los informes `docs/diagnostico-inicial.md`, `docs/backend-etapa3.md`, `docs/apis-externas-etapa4.md` y `docs/integracion-frontend-etapa6.md`.
- Cuando necesites confirmar un requisito, consulta `C:\Users\jaime\Desktop\Estudio de Caso 2.pdf`, `C:\Users\jaime\Desktop\PetCare_Caso_2.pdf` y `C:\Users\jaime\Downloads\Estudio de Caso 2 - Indicaciones para Video Demostración.pdf`.
- Los archivos Postman están en `postman/PetCare.postman_collection.json` y `postman/PetCare_Local.postman_environment.json`.

La referencia de la etapa 6 incluye 46 pruebas Java aprobadas y un JAR empaquetado, 10 comprobaciones en Chrome, cuatro verificaciones simuladas adicionales y 11 recorridos de regresión. Chrome consultó E08 y E09 directamente desde Node con la configuración de Spring. La caída y recuperación de una instancia Node y los avisos de confirmación y cancelación se verificaron con PostgreSQL y Mailpit. Usa estos resultados como referencia histórica y ejecuta las comprobaciones de esta etapa.

## 1. Revisar y levantar el entorno

- Comprueba la rama, los archivos pendientes y los remotos de ambos repositorios. Trabaja en una rama de desarrollo adecuada y conserva los cambios existentes. Los commits y publicaciones de la etapa 7 requieren una solicitud del usuario.
- El entorno se cerró al terminar la etapa anterior. Comprueba los servicios y puertos actuales y levanta los necesarios. Los valores locales habituales son Spring en 8082, Node en 3001, PostgreSQL en 5433 y Mailpit en 8025 con SMTP en 1025.
- Usa Java 21 y `npm.cmd` en PowerShell. Configura Spring con una base de pruebas separada y Node con Mailpit. Usa datos identificados como Etapa7, correos únicos bajo `example.invalid` y horarios futuros. Conserva bases, volúmenes y servicios ajenos al proyecto.
- Define `baseUrl` para Spring y `externalUrl` para Node en el entorno Postman. Usa `EXTERNAL_API_URL` en Spring y comprueba que `GET /api/configuracion` devuelve la dirección pública esperada.

## 2. Actualizar Postman

- Comprueba el método, la ruta, el cuerpo y las respuestas de los diez endpoints E01–E10 contra los controladores y los contratos documentados. Aprovecha la colección actual y añade las rutas auxiliares que se necesiten para verificar el flujo.
- Corrige D03. E09 devuelve cinco campos. Comprueba `nombre`, `direccion`, `latitud`, `longitud` y `urlMapa`, con coordenadas numéricas dentro de rango y un enlace HTTP o HTTPS válido y coherente.
- Corrige D04. Genera `fechaHora` como hora local ISO en la zona de la clínica, America/Santiago para las pruebas de este proyecto. Asegúrate de que sea futura y comprueba la hora guardada. La colección actual convierte el valor a UTC y luego elimina la marca de zona. Genera un sufijo de datos y horarios nuevos al comenzar cada ejecución, y reutilízalos dentro de ese recorrido.
- Guarda los identificadores que devuelve el servidor en cada alta. Comprueba el flujo completo desde cliente y mascota hasta reserva, reprogramación, confirmación, atención e historial. Usa otra reserva para verificar cancelación.
- En E06 valida el estado guardado de la reserva y el resultado inicial asíncrono, que puede ser `PENDIENTE:<claveEvento>`. Consulta `GET /api/reservas/{id}` para ver el aviso persistido, con espera acotada si necesitas verificar su aceptación. `ACEPTADA` acredita aceptación por SMTP. Comprueba aparte el mensaje capturado en Mailpit.
- En E08 verifica una consulta parcial exitosa con los cinco campos, 400 por búsqueda inválida y 404 `SIN_RESULTADOS`. En E10 verifica el 202 con `idNotificacion` y `estado`, la validación 400 y los resultados de idempotencia previstos en el contrato. Usa destinatarios de prueba.
- Incluye los casos de error relevantes de Spring. Comprueba datos inválidos, recurso inexistente, reserva duplicada 409, transición inválida y atención fuera del estado permitido. Valida los campos pertinentes de `ApiError` y evita aserciones que pasen con cualquier respuesta.
- Deja el entorno exportado con direcciones de ejemplo y sin credenciales ni identificadores de una ejecución anterior.

## 3. Ejecutar las verificaciones

- Ejecuta la colección completa con Postman Collection Runner, Postman CLI o Newman según el entorno disponible. Identifica el ejecutor concreto, registra sus resultados y repite con el mismo entorno para comprobar que se generan datos nuevos. Un arnés auxiliar debe identificarse con su nombre si se utiliza para diagnósticos.
- Ejecuta `./gradlew.bat check bootJar` en el principal y `npm.cmd test` en el externo.
- Recorre el sistema en el navegador. Verifica altas y edición, reserva con hora local, rechazo de duplicado, confirmación, cancelación, atención e historial después de recargar. Incluye búsqueda de medicamentos, ubicación y seguimiento de los avisos. Usa la pestaña Red para conservar evidencia de los orígenes de E08 y E09.
- Revisa en Mailpit los destinatarios, asuntos y horarios de los avisos. Verifica que Spring mantiene las operaciones de gestión ante una indisponibilidad de Node y que vuelve a consumir la API tras su recuperación. Usa instancias de prueba que puedas controlar. Registra por separado los fallos provocados realmente y las respuestas simuladas.
- Corrige los defectos reproducibles de esta etapa que bloqueen la verificación y repite las pruebas afectadas. Conserva los contratos E01–E10.

## 4. Entregar y cerrar

- Guarda la colección y el entorno actualizados en `postman/`.
- Crea `docs/verificacion-integral-etapa7.md` con la configuración utilizada, los cambios D03 y D04, resultados de cada ejecución, cobertura E01–E10, evidencia del navegador y de Mailpit, y cualquier comprobación pendiente con su causa.
- Incluye un recorrido breve reutilizable para el video. La guía pide mostrar CRUD, disponibilidad y duplicados, transición de estados, consumo de las APIs y persistencia en PostgreSQL. Reserva la comprobación física de dos computadores para la etapa 8 y la grabación y entrega para la etapa 9. La prueba externa con Gmail se hará si se configura una cuenta y se autoriza el destinatario.
- Informa qué cambió, qué aprobó, cómo repetirlo y qué servicios quedaron activos.

## Criterio de cierre

La colección actualizada puede ejecutarse dos veces con el mismo entorno y datos de prueba distintos. Los contratos y flujos principales se comprueban con las pruebas automatizadas y desde el navegador. El informe distingue los resultados observados, las simulaciones y las verificaciones pendientes de la demostración.
