# Separación de la API externa de PetCare

Etapa 2, realizada el 17 de noviembre de 2026 en America/Santiago, UTC-03:00. Comprobación final de servicios a las 17:06.

## Resultado

PetCare dispone de dos repositorios Git locales independientes. El servicio Node se ejecutó mediante `npm.cmd start` desde su nueva carpeta y Spring consumió sus notificaciones con PostgreSQL. Los tres endpoints externos conservaron sus contratos y los hashes del código trasladado coincidieron con los originales.

| Repositorio | Ubicación absoluta | Rama |
|---|---|---|
| Principal | `C:\Users\jaime\Desktop\JaimePalavecino\Universidad\Codigos\WebOO\petcare-weboo` | `chore/diagnostico-inicial` |
| API externa | `C:\Users\jaime\Desktop\JaimePalavecino\Universidad\Codigos\WebOO\petcare-external-api` | `develop` |

Se continuó en la rama de desarrollo existente del proyecto principal y se seleccionó `develop` para el repositorio nuevo. El repositorio principal partió del commit `120211a`, que contiene el diagnóstico de la etapa 1. Ese documento se conserva como registro histórico.

Los cambios de esta etapa quedaron locales y pendientes de commit. El segundo repositorio tiene su primer commit pendiente y sus cuatro archivos aparecen como nuevos en Git.

## Archivos y estructura

```text
WebOO/
|- petcare-weboo/
|  |- src/
|  |- database/
|  |- postman/
|  |- docs/
|  |  |- diagnostico-inicial.md
|  |  `- separacion-api-externa.md
|  |- README.md
|  |- compose.yaml
|  `- build.gradle
`- petcare-external-api/
   |- .gitignore
   |- README.md
   |- package.json
   `- server.js
```

Los archivos `external-api/server.js` y `external-api/package.json` se copiaron al repositorio hermano. Después de verificar los hashes, los contratos HTTP y la integración, se retiró la carpeta original `petcare-weboo/external-api`.

Se verificó la ruta absoluta antes de la retirada y se comprobó que la carpeta contenía únicamente esos dos archivos. La copia del servicio y sus metadatos se validó mediante SHA-256 antes y después del traslado.

| Archivo | SHA-256 original y copia |
|---|---|
| `server.js` | `03AA7A13CCE93F91637981E18FD1B9A2ACD4FFAB3CD4D857AC7AF19FFA7E67EA` |
| `package.json` | `E13F18743B28BFDED5170DA53DB5BAB90C723F32602BCB2DBFBDE6DD89205A5F` |

En la API externa se añadieron README y `.gitignore`. Se comprobó la exclusión de `.env`, `.env.local`, `node_modules`, registros y archivos de claves mediante `git check-ignore`.

En el README principal se actualizaron la estructura, la ruta del servicio Node y los pasos de ejecución desde las dos carpetas. También se documentaron `EXTERNAL_API_URL`, la configuración de diagnóstico y las variables de Postman.

## Entorno y datos

| Componente | Ejecución comprobada |
|---|---|
| Java | Oracle JDK 21.0.12.1 seleccionado por proceso |
| Gradle | Wrapper 9.7.1 |
| Spring Boot | 4.1.1, puerto 8082 |
| Node | v24.15.0, iniciado con `npm.cmd start` |
| npm | 11.12.1 |
| Docker Engine | 29.9.2 |
| PostgreSQL | Contenedor existente `petcare-postgres`, healthy, puerto 5433 |
| Volumen reutilizado | `petcare-weboo_petcare_data` |
| Base para pruebas HTTP | `petcare_diagnostico_20261117` |
| Base para pruebas automatizadas | H2 en memoria con la configuración de `src/test/resources` |

Docker Desktop y el contenedor PostgreSQL estaban detenidos al comenzar. Se iniciaron para esta etapa y se reutilizaron la base de diagnóstico y el volumen existentes.

Se crearon dos conjuntos de datos identificados mediante `Etapa2` y correos `etapa2.<timestamp>@example.invalid`, uno en cada ejecución HTTP. Ambos conjuntos se conservaron en la base de diagnóstico.

| Ejecución | Cliente | Mascota | Veterinario | Reserva confirmada | Reserva cancelada |
|---|---|---|---|---|---|
| Antes de retirar la carpeta original | 4 | 5 | 6 | 6 | 7 |
| Después de retirar y reiniciar | 5 | 6 | 8 | 8 | 9 |

La consulta SQL final comprobó los estados CONFIRMADA de las reservas 6 y 8 y CANCELADA de las reservas 7 y 9. La base quedó con 5 clientes, 6 mascotas, 6 veterinarios, 9 reservas y 2 atenciones médicas.

## Configuración y comandos de inicio

Se utiliza una terminal por servicio. Los comandos siguientes corresponden al equipo de desarrollo y su instalación actual de Java 21.

### PostgreSQL

Iniciar Docker Desktop y esperar a que el motor esté disponible. Desde la raíz de `petcare-weboo`, ejecutar

```powershell
docker compose up -d postgres
docker compose ps
```

Se verificó la base existente con

```powershell
docker exec petcare-postgres psql -U postgres -d postgres -tAc "SELECT datname FROM pg_database WHERE datname='petcare_diagnostico_20261117'"
```

### API externa

Desde la raíz de `petcare-weboo`, abrir otra terminal y ejecutar

```powershell
Set-Location ..\petcare-external-api
$env:HOST = '127.0.0.1'
$env:PORT = '3001'
npm.cmd start
```

El arranque muestra `petcare-external-api@1.0.0 start`, `node server.js` y la dirección `http://127.0.0.1:3001`.

Se registró el directorio de trabajo con el que se inició npm y se verificó que el PID que escuchaba en 3001 descendía de ese proceso. La prueba final incluyó un arranque nuevo después de retirar la carpeta original.

Las variables se asignan en la terminal y Node las lee desde `process.env`. El README del servicio documenta los valores predeterminados HOST `0.0.0.0` y PORT `3001`, los ejemplos HTTP y el estado demostrativo de los datos.

### Spring Boot

Desde `petcare-weboo`, seleccionar Java 21 y configurar la base de diagnóstico y la URL externa

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:DB_URL = 'jdbc:postgresql://localhost:5433/petcare_diagnostico_20261117'
$env:EXTERNAL_API_URL = 'http://localhost:3001'
```

Durante la verificación se ejecutaron pruebas y empaquetado con el wrapper existente

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar gradle/wrapper/gradle-wrapper.jar check bootJar --rerun-tasks --no-daemon --console=plain
```

El JAR resultante se inició desde la raíz del repositorio

```powershell
$env:SERVER_PORT = '8082'
$env:SERVER_ADDRESS = '127.0.0.1'
& "$env:JAVA_HOME\bin\java.exe" -jar build/libs/petcare-0.0.1-SNAPSHOT.jar
```

La ejecución utilizó las credenciales locales de desarrollo declaradas en el Compose existente, asignadas al proceso. Para el desarrollo cotidiano, el README principal también conserva el inicio mediante `gradlew.bat bootRun` con Java 21 seleccionado.

`EXTERNAL_API_URL` configura las llamadas realizadas desde Spring a Node. Para un segundo equipo, se podrá asignar su dirección accesible antes de iniciar Spring. La variable `externalUrl` de Postman configura las solicitudes directas a Node. La configuración del frontend se realizará durante su implementación.

## Pruebas ejecutadas

### Pruebas automatizadas y empaquetado

Se ejecutaron `check` y `bootJar` con Java 21 antes de retirar la carpeta original y se repitieron después de retirarla. Ambas ejecuciones terminaron con BUILD SUCCESSFUL y siete tareas ejecutadas.

El XML final registra 3 tests, 0 failures, 0 errors y 0 skipped, con marca de tiempo `2026-11-17T20:05:08.457Z`.

| Prueba existente | Resultado final |
|---|---|
| `cargaContextoYControladores()` | Aprobada |
| `endpointsCreanReservaYRechazanHorarioDuplicado()` | Aprobada |
| `reservaConfirmadaRegistraAtencionEHistorial()` | Aprobada |

### Comprobaciones HTTP

Se ejecutaron 24 comprobaciones antes de la retirada y las mismas 24 después de retirar la carpeta original y reiniciar ambos servicios. Las dos ejecuciones aprobaron sus 24 comprobaciones, para un total de 48.

Los casos de compatibilidad D01 esperan el 202 actual para reproducir el comportamiento diagnosticado. Su aprobación acredita la continuidad del contrato durante el traslado. La validación completa corresponde a la etapa 4.

| Comprobación | Código esperado | Antes | Después |
|---|---|---|---|
| E08 y equivalencia con la respuesta de etapa 1 | 200 | Correcto | Correcto |
| Búsqueda parcial en mayúsculas | 200 | Correcto | Correcto |
| Nombre ausente | 400 | Correcto | Correcto |
| Nombre vacío | 400 | Correcto | Correcto |
| Medicamento sin coincidencias | 404 | Correcto | Correcto |
| E09, cinco campos y equivalencia con etapa 1 | 200 | Correcto | Correcto |
| E10, ACEPTADA y formato UUID | 202 | Correcto | Correcto |
| Todos los campos ausentes | 400 | Correcto | Correcto |
| Un campo ausente | 400 | Correcto | Correcto |
| JSON inválido | 400 | Correcto | Correcto |
| Compatibilidad D01, campos nulos | 202 | Correcto | Correcto |
| Compatibilidad D01, tipos inválidos | 202 | Correcto | Correcto |
| OPTIONS y cabeceras CORS | 204 | Correcto | Correcto |
| Ruta desconocida | 404 | Correcto | Correcto |
| Creación de cliente de etapa 2 | 201 | Correcto | Correcto |
| Creación de mascota de etapa 2 | 201 | Correcto | Correcto |
| Creación de veterinario de etapa 2 | 201 | Correcto | Correcto |
| Reserva para confirmar | 201 | Correcto | Correcto |
| Confirmación mediante Node separado | 200 y ACEPTADA con UUID | Correcto | Correcto |
| Consulta de confirmación persistida | 200 y CONFIRMADA | Correcto | Correcto |
| Reserva para cancelar | 201 | Correcto | Correcto |
| Cancelación mediante Node separado | 200 y ACEPTADA con UUID | Correcto | Correcto |
| Consulta de cancelación persistida | 200 y CANCELADA | Correcto | Correcto |
| Confirmación repetida | 200 y NO_APLICA | Correcto | Correcto |

Medicamentos y ubicación se compararon con los cuerpos guardados en la evidencia de la etapa 1. La comparación fue exacta para ambos resultados. Las comprobaciones HTTP de integración utilizaron PostgreSQL y el Node del repositorio nuevo.

## Diferencias y pendientes respecto al diagnóstico inicial

| Tema | Situación después de la etapa 2 |
|---|---|
| Ubicación de Node | Repositorio hermano `petcare-external-api` con rama `develop` |
| Carpeta `external-api` principal | Retirada después de validar la separación |
| Inicio de Node | Verificado con `npm.cmd start` en el repositorio nuevo |
| Documentación de ejecución | README principal actualizado y README propio para la API |
| Notificaciones | Aceptación simulada mediante UUID, con envío real pendiente para etapa 4 |
| Validación D01 y representación de reintentos | Pendientes de etapa 4 |
| Manejo HTTP D02 y consultas pendientes de Spring | Pendientes de etapa 3 |
| Aserción de ubicación y fechas de Postman | Pendientes de etapa 7 |
| Frontend | Pendiente de etapa 5 y su integración en etapa 6 |
| Segundo equipo/red | Demostración pendiente para la fase correspondiente |

Las referencias activas de ejecución se revisaron en README, configuración, código y Postman. El diagnóstico inicial conserva las rutas que correspondían al momento de su elaboración.

La separación quedó verificada y permite continuar con la etapa 3. Los pendientes funcionales están registrados en las etapas correspondientes.

## Estado final de servicios

| Servicio | Acciones durante esta etapa | Estado al cerrar |
|---|---|---|
| Docker Desktop y motor | Iniciados para las pruebas | Activos |
| PostgreSQL `petcare-postgres` | Iniciado reutilizando el volumen | Activo y healthy, puerto 5433 |
| Node y procesos npm asociados | Iniciados desde el repositorio hermano, reiniciados para validación final | Detenidos al terminar, puerto 3001 libre |
| Spring Boot | Iniciado con la base de diagnóstico, reiniciado para validación final | Detenido al terminar, puerto 8082 libre |

Se verificó la identidad de los procesos propios antes de detenerlos. La base de diagnóstico y sus datos quedan disponibles para revisar las reservas creadas en esta etapa.

## Evidencia local

Los siguientes archivos están bajo el directorio `build`, ignorado por Git

- `build/etapa2/integridad.json` con los hashes de origen y copia.
- `build/etapa2/http-before-removal.json` con las primeras 24 comprobaciones.
- `build/etapa2/http-after-removal.json` con las 24 comprobaciones finales.
- `build/etapa2/state-before-removal.json` y `state-after-removal.json` con los identificadores creados.
- `build/etapa2/node-processes-before.json` y `node-processes-final.json` con la cadena de procesos npm y Node.
- `build/etapa2/services-end.json` con la comprobación final de servicios.
- `build/etapa2/node*.log` y `spring*.log` con los registros de los procesos.
- `build/test-results/test/TEST-cl.bello.petcare.PetcareApplicationTests.xml` con el resultado final de pruebas.
- `build/reports/tests/test/index.html` con el reporte de Gradle.

Este informe contiene los resultados esenciales para revisar la separación. Los archivos de `build` corresponden a la evidencia de ejecución local y pueden consultarse mientras estén disponibles.
