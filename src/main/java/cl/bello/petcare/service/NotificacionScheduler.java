package cl.bello.petcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "petcare.notifications", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class NotificacionScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificacionScheduler.class);
	private final NotificacionDispatcher dispatcher;

	public NotificacionScheduler(NotificacionDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Scheduled(fixedDelayString = "${petcare.notifications.worker-delay-ms:5000}",
			initialDelayString = "${petcare.notifications.worker-initial-delay-ms:5000}")
	public void procesar() {
		try {
			dispatcher.procesarPendientes();
		} catch (RuntimeException error) {
			LOGGER.error("Error al procesar avisos pendientes", error);
		}
	}
}
