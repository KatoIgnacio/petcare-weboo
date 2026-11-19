package cl.bello.petcare.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
class HorarioClinica {

	private static final ZoneId ZONA = ZoneId.of("America/Santiago");
	private static final LocalTime APERTURA = LocalTime.of(8, 30);
	private static final LocalTime CIERRE = LocalTime.of(19, 0);
	private static final int DURACION_MINUTOS = 30;
	private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm");
	private static final List<LocalTime> BLOQUES = IntStream.range(0,
			(CIERRE.toSecondOfDay() - APERTURA.toSecondOfDay()) / (60 * DURACION_MINUTOS))
			.mapToObj(bloque -> APERTURA.plusMinutes((long) bloque * DURACION_MINUTOS))
			.toList();

	private final Clock reloj;

	HorarioClinica(Clock reloj) {
		this.reloj = reloj;
	}

	void validar(LocalDateTime inicio) {
		if (!inicio.isAfter(ahora())) {
			throw new IllegalArgumentException("La fecha y hora deben estar en el futuro");
		}
		if (!BLOQUES.contains(inicio.toLocalTime())) {
			throw new IllegalArgumentException("Selecciona un bloque de 30 minutos entre las 08:30 y las 18:30");
		}
	}

	List<String> horasLibres(LocalDate dia, List<LocalDateTime> ocupadas) {
		LocalDateTime ahora = ahora();
		return BLOQUES.stream().map(dia::atTime)
				.filter(inicio -> inicio.isAfter(ahora))
				.filter(inicio -> ocupadas.stream().noneMatch(otra -> otra.isAfter(limiteInferior(inicio))
						&& otra.isBefore(fin(inicio))))
				.map(FORMATO::format)
				.toList();
	}

	LocalDateTime desde(LocalDate dia) {
		return limiteInferior(dia.atTime(APERTURA));
	}

	LocalDateTime hasta(LocalDate dia) {
		return dia.atTime(CIERRE);
	}

	LocalDateTime limiteInferior(LocalDateTime inicio) {
		return inicio.minusMinutes(DURACION_MINUTOS);
	}

	LocalDateTime fin(LocalDateTime inicio) {
		return inicio.plusMinutes(DURACION_MINUTOS);
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(reloj.instant(), ZONA);
	}
}
