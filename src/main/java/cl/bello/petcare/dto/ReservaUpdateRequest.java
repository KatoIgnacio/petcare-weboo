package cl.bello.petcare.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservaUpdateRequest(
		@NotNull Long veterinarioId,
		@NotNull LocalDateTime fechaHora) {
}

