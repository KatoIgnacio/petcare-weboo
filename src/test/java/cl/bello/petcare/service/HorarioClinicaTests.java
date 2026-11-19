package cl.bello.petcare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorarioClinicaTests {

	@Test
	void usaLaHoraDeSantiagoYNoOfreceUnBloqueQueYaEmpezo() {
		var horario = new HorarioClinica(Clock.fixed(Instant.parse("2026-11-19T13:00:00Z"), ZoneOffset.UTC));
		var dia = LocalDate.of(2026, 11, 19); // En Santiago son las 10:00.
		assertThat(horario.horasLibres(dia, List.of())).startsWith("10:30").endsWith("18:30")
				.doesNotContain("09:30", "10:00");
		assertThatThrownBy(() -> horario.validar(dia.atTime(10, 0)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("futuro");
		assertThat(horario.horasLibres(dia.minusDays(1), List.of())).isEmpty();
	}
}
