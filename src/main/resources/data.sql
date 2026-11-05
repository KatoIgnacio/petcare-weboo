INSERT INTO veterinarios (nombre, especialidad, email, activo)
VALUES ('Dra. Paula Rojas', 'Medicina general', 'paula.rojas@petcare.cl', TRUE)
ON CONFLICT (email) DO NOTHING;

