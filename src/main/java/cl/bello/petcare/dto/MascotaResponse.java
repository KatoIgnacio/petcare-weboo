package cl.bello.petcare.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaResponse(
		Long id,
		Long clienteId,
		String nombre,
		String especie,
		String raza,
		LocalDate fechaNacimiento,
		BigDecimal peso,
		boolean activo) {
}

