package cl.bello.petcare.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservaRequest(
		@NotNull Long mascotaId,
		@NotNull Long veterinarioId,
		@NotNull @Future LocalDateTime fechaHora) {
}

