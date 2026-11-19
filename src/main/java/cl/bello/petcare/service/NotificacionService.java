package cl.bello.petcare.service;

import cl.bello.petcare.entity.NotificacionPendiente;
import cl.bello.petcare.entity.Reserva;
import cl.bello.petcare.entity.Veterinario;
import cl.bello.petcare.repository.NotificacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {
	private final NotificacionRepository repository;
	private final Clock clock;

	public NotificacionService(NotificacionRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String registrarCambio(Reserva reserva) {
		NotificacionPendiente evento = repository.save(new NotificacionPendiente(reserva, Instant.now(clock)));
		return evento.resultado();
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String registrarReprogramacion(Reserva reserva, LocalDateTime horaAnterior, Veterinario veterinarioAnterior) {
		return repository.save(NotificacionPendiente.reprogramacion(reserva, horaAnterior, veterinarioAnterior,
				Instant.now(clock))).resultado();
	}

	@Transactional(readOnly = true)
	public String resultado(Long reservaId) {
		return repository.findFirstByReservaIdOrderByIdDesc(reservaId)
				.map(NotificacionPendiente::resultado).orElse("NO_APLICA");
	}
}
