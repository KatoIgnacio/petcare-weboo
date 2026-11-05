CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    telefono VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS mascotas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    nombre VARCHAR(100) NOT NULL,
    especie VARCHAR(80) NOT NULL,
    raza VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    peso NUMERIC(7,2) NOT NULL CHECK (peso > 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS veterinarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    especialidad VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS reservas (
    id BIGSERIAL PRIMARY KEY,
    mascota_id BIGINT NOT NULL REFERENCES mascotas(id),
    veterinario_id BIGINT NOT NULL REFERENCES veterinarios(id),
    fecha_hora TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'ATENDIDA')),
    creada_en TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS atenciones_medicas (
    id BIGSERIAL PRIMARY KEY,
    reserva_id BIGINT NOT NULL UNIQUE REFERENCES reservas(id),
    fecha TIMESTAMP NOT NULL,
    diagnostico VARCHAR(1000) NOT NULL,
    tratamiento VARCHAR(1000) NOT NULL,
    observaciones VARCHAR(1500)
);

CREATE TABLE IF NOT EXISTS atencion_medicamentos (
    atencion_id BIGINT NOT NULL REFERENCES atenciones_medicas(id) ON DELETE CASCADE,
    medicamento VARCHAR(120) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mascotas_cliente ON mascotas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_reservas_veterinario_fecha ON reservas(veterinario_id, fecha_hora);
CREATE INDEX IF NOT EXISTS idx_reservas_mascota_fecha ON reservas(mascota_id, fecha_hora);

