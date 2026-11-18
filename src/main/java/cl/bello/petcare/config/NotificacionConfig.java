package cl.bello.petcare.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificacionProperties.class)
public class NotificacionConfig {
	@Bean
	Clock notificationClock() {
		return Clock.systemUTC();
	}
}
