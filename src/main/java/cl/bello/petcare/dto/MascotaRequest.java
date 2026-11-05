package cl.bello.petcare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MascotaRequest(
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(max = 80) String especie,
		@NotBlank @Size(max = 100) String raza,
		@NotNull @PastOrPresent LocalDate fechaNacimiento,
		@NotNull @DecimalMin(value = "0.1") BigDecimal peso) {
}

