package cl.bello.petcare.service;

import cl.bello.petcare.config.NotificacionProperties;
import cl.bello.petcare.dto.NotificacionRequest;
import cl.bello.petcare.entity.EstadoNotificacion;
import cl.bello.petcare.entity.NotificacionPendiente;
import cl.bello.petcare.repository.NotificacionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificacionDispatcher {
	private final NotificacionRepository repository;
	private final NotificacionClient client;
	private final NotificacionProperties properties;
	private final Clock clock;
	private final TransactionTemplate transaction;

	public NotificacionDispatcher(NotificacionRepository repository, NotificacionClient client,
			NotificacionProperties properties, Clock clock, PlatformTransactionManager manager) {
		this.repository = repository;
		this.client = client;
		this.properties = properties;
		this.clock = clock;
		transaction = new TransactionTemplate(manager);
		transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void procesarPendientes() {
		List<Long> pendientes = repository.buscarDisponibles(
				List.of(EstadoNotificacion.PENDIENTE, EstadoNotificacion.PENDIENTE_REINTENTO),
				EstadoNotificacion.EN_PROCESO, Instant.now(clock), PageRequest.of(0, properties.batchSize()));
		pendientes.forEach(this::procesar);
	}

	public void procesar(Long id) {
		Intento intento = transaction.execute(status -> reclamar(id));
		if (intento == null) return;
		NotificacionClient.ResultadoEnvio resultado = client.enviar(intento.clave(), intento.solicitud());
		transaction.executeWithoutResult(status -> finalizar(intento, resultado));
	}

	private Intento reclamar(Long id) {
		NotificacionPendiente evento = repository.buscarParaActualizar(id).orElse(null);
		Instant ahora = Instant.now(clock);
		if (evento == null || !evento.disponible(ahora)) return null;
		if (evento.getIntentos() >= properties.maxAttempts()) {
			boolean interrumpido = evento.getEstado() == EstadoNotificacion.EN_PROCESO;
			evento.completar(interrumpido ? EstadoNotificacion.INCIERTA : EstadoNotificacion.FALLIDA,
					null, interrumpido ? "INTENTO_INTERRUMPIDO" : "INTENTOS_AGOTADOS", ahora, ahora);
			return null;
		}
		evento.iniciarIntento(ahora, properties.leaseDuration());
		return new Intento(evento.getId(), evento.getTokenIntento(), evento.getClaveEvento(), evento.solicitud());
	}

	private void finalizar(Intento intento, NotificacionClient.ResultadoEnvio resultado) {
		NotificacionPendiente evento = repository.buscarParaActualizar(intento.id()).orElseThrow();
		if (!Objects.equals(evento.getTokenIntento(), intento.token())) return;
		Instant ahora = Instant.now(clock);
		EstadoNotificacion estado = resultado.estado();
		if (estado == EstadoNotificacion.PENDIENTE_REINTENTO && evento.getIntentos() >= properties.maxAttempts()) {
			estado = EstadoNotificacion.FALLIDA;
		}
		evento.completar(estado, resultado.idExterno(), resultado.error(), ahora.plus(espera(evento.getIntentos())), ahora);
	}

	private Duration espera(int intentos) {
		long base = properties.retryDelay().toMillis();
		long maximo = properties.maxRetryDelay().toMillis();
		long factor = 1L << Math.min(intentos - 1, 20);
		return Duration.ofMillis(base > maximo / factor ? maximo : base * factor);
	}

	private record Intento(Long id, String token, String clave, NotificacionRequest solicitud) {
	}
}
