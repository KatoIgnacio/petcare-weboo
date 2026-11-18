package cl.bello.petcare.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("petcare.notifications")
public record NotificacionProperties(
		@DefaultValue("5") int maxAttempts,
		@DefaultValue("30s") Duration retryDelay,
		@DefaultValue("5m") Duration maxRetryDelay,
		@DefaultValue("30s") Duration leaseDuration,
		@DefaultValue("10") int batchSize) {
	public NotificacionProperties {
		if (maxAttempts < 1 || batchSize < 1 || retryDelay.isNegative() || retryDelay.isZero()
				|| leaseDuration.isNegative() || leaseDuration.isZero() || maxRetryDelay.compareTo(retryDelay) < 0) {
			throw new IllegalArgumentException("La política de notificaciones requiere intentos, lote y duraciones positivos");
		}
	}
}
