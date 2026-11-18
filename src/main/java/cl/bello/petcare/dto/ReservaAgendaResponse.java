package cl.bello.petcare.dto;

import cl.bello.petcare.entity.EstadoReserva;
import java.time.LocalDateTime;

public record ReservaAgendaResponse(
		Long id,
		Long mascotaId,
		String mascotaNombre,
		Long clienteId,
		String clienteNombre,
		Long veterinarioId,
		String veterinarioNombre,
		LocalDateTime fechaHora,
		EstadoReserva estado) {
}
