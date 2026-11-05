package cl.bello.petcare.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AtencionResponse(
		Long id,
		Long reservaId,
		LocalDateTime fecha,
		String diagnostico,
		String tratamiento,
		List<String> medicamentos,
		String observaciones) {
}

