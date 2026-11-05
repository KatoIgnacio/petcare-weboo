package cl.bello.petcare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AtencionRequest(
		@NotBlank @Size(max = 1000) String diagnostico,
		@NotBlank @Size(max = 1000) String tratamiento,
		@NotNull List<@NotBlank @Size(max = 120) String> medicamentos,
		@Size(max = 1500) String observaciones) {
}

