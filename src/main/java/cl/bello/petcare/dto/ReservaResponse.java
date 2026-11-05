package cl.bello.petcare.dto;

import cl.bello.petcare.entity.EstadoReserva;
import java.time.LocalDateTime;

public record ReservaResponse(
		Long id,
		Long mascotaId,
		String mascotaNombre,
		Long clienteId,
		Long veterinarioId,
		String veterinarioNombre,
		LocalDateTime fechaHora,
		EstadoReserva estado,
		String resultadoNotificacion,
		AtencionResponse atencion) {
}

