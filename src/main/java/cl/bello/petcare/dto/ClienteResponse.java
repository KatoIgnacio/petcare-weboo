package cl.bello.petcare.dto;

public record ClienteResponse(
		Long id,
		String nombre,
		String email,
		String telefono,
		boolean activo) {
}

