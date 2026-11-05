package cl.bello.petcare.dto;

public record NotificacionRequest(
		String destinatario,
		String canal,
		String asunto,
		String mensaje,
		Long reservaId) {
}

