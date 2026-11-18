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

CREATE TABLE IF NOT EXISTS notificaciones_pendientes (
    id BIGSERIAL PRIMARY KEY,
    clave_evento VARCHAR(36) NOT NULL UNIQUE,
    reserva_id BIGINT NOT NULL REFERENCES reservas(id),
    estado_reserva VARCHAR(20) NOT NULL CHECK (estado_reserva IN ('CONFIRMADA', 'CANCELADA')),
    destinatario VARCHAR(254) NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    mensaje VARCHAR(10000) NOT NULL,
    estado VARCHAR(30) NOT NULL CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'PENDIENTE_REINTENTO', 'ACEPTADA', 'FALLIDA', 'INCIERTA')),
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento TIMESTAMP WITH TIME ZONE NOT NULL,
    bloqueada_hasta TIMESTAMP WITH TIME ZONE,
    token_intento VARCHAR(36),
    id_externo VARCHAR(100),
    ultimo_error VARCHAR(200),
    creada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_notificacion_reserva_estado UNIQUE (reserva_id, estado_reserva)
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_estado_fecha
    ON notificaciones_pendientes(estado, proximo_intento);

