package cl.bello.petcare.dto;

public record MedicamentoResponse(
		String nombre,
		String principioActivo,
		String presentacion,
		String advertencias,
		String fuente) {
}
