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
- Servicio Node para medicamentos, ubicación y aceptación simulada de notificaciones, en el repositorio hermano `petcare-external-api`.
- Pruebas automatizadas del flujo principal.
- Flujo integrado verificado con PostgreSQL 17 y la API externa en ejecución.

## Puertos

| Componente | Puerto |
|---|---:|
| Backend Spring Boot | 8082 |
| API externa Node | 3001 |
| PostgreSQL | 5433 |

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
$env:HOST = '127.0.0.1'
$env:PORT = '3001'
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

La aplicación queda disponible en `http://localhost:8082`.

### Configuración de la API externa

`EXTERNAL_API_URL` define la dirección que Spring utiliza para consumir Node y tiene el valor predeterminado `http://localhost:3001`. Para ejecutar Node en otro equipo, asignar la URL de ese equipo antes de iniciar Spring, por ejemplo `http://192.168.1.25:3001`.

Las variables se asignan en la terminal que inicia cada proceso. Spring lee `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `EXTERNAL_API_URL` y `CORS_ALLOWED_ORIGINS` según `application.properties`. Node lee `HOST` y `PORT` según su README.

Para repetir verificaciones sobre la base separada del diagnóstico, asignar también la siguiente variable en la terminal de Spring antes de iniciar la aplicación.

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5433/petcare_diagnostico_20261117'
```

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

Resultado actual: `3` pruebas automatizadas aprobadas y empaquetado `bootJar` exitoso.

## Postman

Importar los dos archivos ubicados en `postman/`, seleccionar el entorno `PetCare Local` y ejecutar la colección completa en orden.

`baseUrl` apunta a Spring y `externalUrl` al servicio Node. El diagnóstico inicial documenta una aserción pendiente en ubicación y el tratamiento de fechas de la colección. Los resultados de la separación se encuentran en [el informe de la etapa 2](docs/separacion-api-externa.md).

## Nota sobre medicamentos

Los datos del servicio Node son demostrativos para probar la integración técnica. No constituyen indicaciones médicas ni reemplazan el criterio profesional veterinario.

