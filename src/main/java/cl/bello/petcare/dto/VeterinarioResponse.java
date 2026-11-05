package cl.bello.petcare.dto;

public record VeterinarioResponse(
		Long id,
		String nombre,
		String especialidad,
		String email,
		boolean activo) {
}

