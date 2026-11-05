BEGIN;

-- Clientes de demostracion. El correo permite volver a ejecutar el script
-- sin duplicar registros.
INSERT INTO clientes (nombre, email, telefono, activo)
VALUES
    ('Carlos Mendez', 'carlos.mendez@demo.petcare.cl', '+56922223333', TRUE),
    ('Fernanda Silva', 'fernanda.silva@demo.petcare.cl', '+56944445555', TRUE),
    ('Maria Gonzalez', 'maria.gonzalez@demo.petcare.cl', '+56966667777', TRUE),
    ('Pedro Soto', 'pedro.soto@demo.petcare.cl', '+56988889999', TRUE),
    ('Daniela Munoz', 'daniela.munoz@demo.petcare.cl', '+56912344321', TRUE)
ON CONFLICT (email) DO UPDATE
SET nombre = EXCLUDED.nombre,
    telefono = EXCLUDED.telefono,
    activo = TRUE;

-- Veterinarios con distintas especialidades.
INSERT INTO veterinarios (nombre, especialidad, email, activo)
VALUES
    ('Dra. Camila Fuentes', 'Dermatologia veterinaria', 'camila.fuentes@demo.petcare.cl', TRUE),
    ('Dr. Diego Morales', 'Cirugia veterinaria', 'diego.morales@demo.petcare.cl', TRUE),
    ('Dra. Valentina Perez', 'Animales exoticos', 'valentina.perez@demo.petcare.cl', TRUE),
    ('Dr. Jose Contreras', 'Cardiologia veterinaria', 'jose.contreras@demo.petcare.cl', TRUE)
ON CONFLICT (email) DO UPDATE
SET nombre = EXCLUDED.nombre,
    especialidad = EXCLUDED.especialidad,
    activo = TRUE;

-- Mascotas asociadas a los clientes anteriores.
INSERT INTO mascotas (cliente_id, nombre, especie, raza, fecha_nacimiento, peso, activo)
SELECT c.id, datos.nombre, datos.especie, datos.raza, datos.fecha_nacimiento, datos.peso, TRUE
FROM (VALUES
    ('carlos.mendez@demo.petcare.cl', 'Max', 'Canina', 'Labrador', DATE '2020-03-15', 28.40),
    ('carlos.mendez@demo.petcare.cl', 'Mishi', 'Felina', 'Domestico de pelo corto', DATE '2022-08-20', 4.60),
    ('fernanda.silva@demo.petcare.cl', 'Rocky', 'Canina', 'Beagle', DATE '2019-11-02', 14.20),
    ('maria.gonzalez@demo.petcare.cl', 'Nala', 'Conejo', 'Mini Lop', DATE '2023-04-12', 2.10),
    ('pedro.soto@demo.petcare.cl', 'Toby', 'Canina', 'Poodle', DATE '2021-06-30', 8.70),
    ('pedro.soto@demo.petcare.cl', 'Kiara', 'Felina', 'Siames', DATE '2018-01-18', 5.20),
    ('daniela.munoz@demo.petcare.cl', 'Copito', 'Cobayo', 'Americano', DATE '2024-02-05', 1.10)
) AS datos(email_cliente, nombre, especie, raza, fecha_nacimiento, peso)
JOIN clientes c ON c.email = datos.email_cliente
WHERE NOT EXISTS (
    SELECT 1
    FROM mascotas m
    WHERE m.cliente_id = c.id
      AND m.nombre = datos.nombre
);

-- Reservas en distintos estados para demostrar el flujo del sistema.
INSERT INTO reservas (mascota_id, veterinario_id, fecha_hora, estado, creada_en, version)
SELECT m.id, v.id, datos.fecha_hora, datos.estado, datos.fecha_hora - INTERVAL '3 days', 0
FROM (VALUES
    ('carlos.mendez@demo.petcare.cl', 'Max', 'paula.rojas@petcare.cl', TIMESTAMP '2026-11-01 09:00:00', 'ATENDIDA'),
    ('carlos.mendez@demo.petcare.cl', 'Mishi', 'camila.fuentes@demo.petcare.cl', TIMESTAMP '2026-11-02 11:00:00', 'ATENDIDA'),
    ('fernanda.silva@demo.petcare.cl', 'Rocky', 'diego.morales@demo.petcare.cl', TIMESTAMP '2026-11-03 15:00:00', 'CANCELADA'),
    ('maria.gonzalez@demo.petcare.cl', 'Nala', 'valentina.perez@demo.petcare.cl', TIMESTAMP '2026-11-06 12:00:00', 'CONFIRMADA'),
    ('pedro.soto@demo.petcare.cl', 'Toby', 'jose.contreras@demo.petcare.cl', TIMESTAMP '2026-11-07 10:00:00', 'PENDIENTE'),
    ('pedro.soto@demo.petcare.cl', 'Kiara', 'camila.fuentes@demo.petcare.cl', TIMESTAMP '2026-11-08 16:00:00', 'CONFIRMADA'),
    ('daniela.munoz@demo.petcare.cl', 'Copito', 'valentina.perez@demo.petcare.cl', TIMESTAMP '2026-11-09 09:00:00', 'PENDIENTE'),
    ('carlos.mendez@demo.petcare.cl', 'Max', 'diego.morales@demo.petcare.cl', TIMESTAMP '2026-11-10 14:00:00', 'CONFIRMADA'),
    ('fernanda.silva@demo.petcare.cl', 'Rocky', 'paula.rojas@petcare.cl', TIMESTAMP '2026-11-11 17:00:00', 'PENDIENTE')
) AS datos(email_cliente, mascota, email_veterinario, fecha_hora, estado)
JOIN clientes c ON c.email = datos.email_cliente
JOIN mascotas m ON m.cliente_id = c.id AND m.nombre = datos.mascota
JOIN veterinarios v ON v.email = datos.email_veterinario
WHERE NOT EXISTS (
    SELECT 1
    FROM reservas r
    WHERE r.mascota_id = m.id
      AND r.veterinario_id = v.id
      AND r.fecha_hora = datos.fecha_hora
);

-- Historial medico para las reservas atendidas.
INSERT INTO atenciones_medicas (reserva_id, fecha, diagnostico, tratamiento, observaciones)
SELECT r.id, datos.fecha, datos.diagnostico, datos.tratamiento, datos.observaciones
FROM (VALUES
    ('carlos.mendez@demo.petcare.cl', 'Max', TIMESTAMP '2026-11-01 09:00:00', TIMESTAMP '2026-11-01 09:35:00',
     'Otitis externa leve', 'Limpieza auricular y gotas por siete dias', 'Control en dos semanas'),
    ('carlos.mendez@demo.petcare.cl', 'Mishi', TIMESTAMP '2026-11-02 11:00:00', TIMESTAMP '2026-11-02 11:30:00',
     'Dermatitis alergica', 'Dieta hipoalergenica y tratamiento topico', 'Observar evolucion durante diez dias')
) AS datos(email_cliente, mascota, fecha_reserva, fecha, diagnostico, tratamiento, observaciones)
JOIN clientes c ON c.email = datos.email_cliente
JOIN mascotas m ON m.cliente_id = c.id AND m.nombre = datos.mascota
JOIN reservas r ON r.mascota_id = m.id AND r.fecha_hora = datos.fecha_reserva
WHERE NOT EXISTS (
    SELECT 1
    FROM atenciones_medicas a
    WHERE a.reserva_id = r.id
);

-- Medicamentos indicados en los historiales anteriores.
INSERT INTO atencion_medicamentos (atencion_id, medicamento)
SELECT a.id, datos.medicamento
FROM (VALUES
    ('carlos.mendez@demo.petcare.cl', 'Max', TIMESTAMP '2026-11-01 09:00:00', 'Solucion de limpieza auricular'),
    ('carlos.mendez@demo.petcare.cl', 'Max', TIMESTAMP '2026-11-01 09:00:00', 'Gotas oticas veterinarias'),
    ('carlos.mendez@demo.petcare.cl', 'Mishi', TIMESTAMP '2026-11-02 11:00:00', 'Shampoo hipoalergenico'),
    ('carlos.mendez@demo.petcare.cl', 'Mishi', TIMESTAMP '2026-11-02 11:00:00', 'Clorhexidina')
) AS datos(email_cliente, mascota, fecha_reserva, medicamento)
JOIN clientes c ON c.email = datos.email_cliente
JOIN mascotas m ON m.cliente_id = c.id AND m.nombre = datos.mascota
JOIN reservas r ON r.mascota_id = m.id AND r.fecha_hora = datos.fecha_reserva
JOIN atenciones_medicas a ON a.reserva_id = r.id
WHERE NOT EXISTS (
    SELECT 1
    FROM atencion_medicamentos am
    WHERE am.atencion_id = a.id
      AND am.medicamento = datos.medicamento
);

COMMIT;
