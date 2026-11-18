# PetCare

Sistema de gestión de atención veterinaria desarrollado para la asignatura Programación Web Orientada a Objetos.

## Estado del avance

- Backend Spring Boot 4.1.1 con Java 21.
- Arquitectura `Controller -> Service -> Repository`.
- Modelo relacional para clientes, mascotas, veterinarios, reservas y atenciones médicas.
- PostgreSQL mediante Docker Compose.
- Validación de datos y manejo uniforme de errores HTTP.
- Prevención de reservas duplicadas para el veterinario o la mascota.
- Estados `PENDIENTE`, `CONFIRMADA`, `CANCELADA` y `ATENDIDA`.
- Servicio Node independiente para medicamentos, ubicación y notificaciones.
- Interfaz web Thymeleaf para clientes, mascotas, reservas, atenciones, veterinarios e integraciones.
- Dashboard responsivo con la identidad visual de PetCare.
- Pruebas automatizadas del flujo principal.
- Flujo local integrado con PostgreSQL 17 y la API externa Node.

## Puertos

| Componente | Puerto |
|---|---:|
| Backend Spring Boot | 8082 |
| API externa Node | 3001 |
| PostgreSQL | 5433 |

## Estructura principal

```text
petcare/
|- src/main/java/cl/bello/petcare/
|  |- controller/
|  |- service/
|  |- repository/
|  |- entity/
|  |- dto/
|  |- exception/
|  `- config/
|- src/main/resources/
|  |- schema.sql
|  |- data.sql
|  |- application.properties
|  |- templates/
|  `- static/
|- external-api/
|- postman/
`- compose.yaml
```

## Ejecución

### 1. Iniciar Docker Desktop

Esperar hasta que el motor de Docker aparezca activo.

### 2. Iniciar PostgreSQL

```powershell
cd C:\Users\KATO\Desktop\WebOS\petcare
docker compose up -d
docker compose ps
```

El contenedor `petcare-postgres` debe aparecer como `healthy`.

### 3. Iniciar la API externa

En una segunda terminal:

```powershell
cd C:\Users\KATO\Desktop\WebOS\petcare\external-api
npm start
```

Se debe mostrar `PetCare external API disponible`.

### 4. Iniciar Spring Boot

En una tercera terminal:

```powershell
cd C:\Users\KATO\Desktop\WebOS\petcare
.\gradlew.bat bootRun
```

La aplicación queda disponible en `http://localhost:8082`.

La ejecución evaluada se realizará en un mismo computador. La variable
`EXTERNAL_API_URL` permite cambiar posteriormente la ubicación de la API Node,
pero la prueba entre dos computadores queda fuera de esta entrega.

## Endpoints principales del reporte

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

### Endpoints REST completos

| Recurso | Método y ruta |
|---|---|
| Clientes | `POST /api/clientes` |
| Clientes | `GET /api/clientes` |
| Clientes | `GET /api/clientes/{id}` |
| Clientes | `PUT /api/clientes/{id}` |
| Clientes | `DELETE /api/clientes/{id}` |
| Mascotas | `POST /api/clientes/{clienteId}/mascotas` |
| Mascotas | `GET /api/clientes/{clienteId}/mascotas` |
| Mascotas | `GET /api/mascotas` |
| Mascotas | `GET /api/mascotas/{id}` |
| Mascotas | `GET /api/mascotas/{id}/historial` |
| Mascotas | `PUT /api/mascotas/{id}` |
| Mascotas | `DELETE /api/mascotas/{id}` |
| Veterinarios | `POST /api/veterinarios` |
| Veterinarios | `GET /api/veterinarios` |
| Reservas | `POST /api/reservas` |
| Reservas | `GET /api/reservas` |
| Reservas | `GET /api/reservas/{id}` |
| Reservas | `PUT /api/reservas/{id}` |
| Reservas | `PATCH /api/reservas/{id}/estado` |
| Reservas | `POST /api/reservas/{id}/atencion` |
| Integraciones | `GET /api/integraciones/medicamentos?nombre={nombre}` |
| Integraciones | `GET /api/integraciones/ubicacion-clinica` |

## Base de datos

`schema.sql` crea las tablas y restricciones. `data.sql` agrega un veterinario demostrativo:

- Email: `paula.rojas@petcare.cl`
- Especialidad: Medicina general

La eliminación de clientes y mascotas es lógica para conservar el historial.

### Datos de demostración ampliados

El archivo `database/demo-data.sql` agrega clientes, mascotas, veterinarios,
reservas en distintos estados, incluidas reservas futuras para la demostración,
atenciones y medicamentos. Es idempotente: se
puede ejecutar nuevamente sin duplicar el conjunto de demostración.

```powershell
docker cp database/demo-data.sql petcare-postgres:/tmp/demo-data.sql
docker exec petcare-postgres psql -U postgres -d petcare -f /tmp/demo-data.sql
```

## Pruebas

```powershell
cd C:\Users\KATO\Desktop\WebOS\petcare
.\gradlew.bat check
```

Las pruebas verifican:

- Carga del contexto y controladores.
- Creación HTTP de cliente, mascota, veterinario y reserva.
- Rechazo HTTP de una reserva duplicada.
- Confirmación de reserva y registro de atención médica.
- Renderización de las páginas principales y disponibilidad del GIF del dashboard.

Resultado actual: `4` pruebas automatizadas aprobadas y empaquetado `bootJar` exitoso.

## Postman

Importar los dos archivos ubicados en `postman/`, seleccionar el entorno
`PetCare Local` y ejecutar la colección completa en orden. La colección cubre
los 22 endpoints Spring y los 3 endpoints directos de la API Node.

## Nota sobre medicamentos

Los datos del servicio Node son demostrativos para probar la integración técnica. No constituyen indicaciones médicas ni reemplazan el criterio profesional veterinario.

