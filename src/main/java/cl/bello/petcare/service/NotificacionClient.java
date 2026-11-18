package cl.bello.petcare.service;

import cl.bello.petcare.dto.NotificacionRequest;
import cl.bello.petcare.dto.NotificacionResponse;
import cl.bello.petcare.entity.EstadoNotificacion;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class NotificacionClient {

	private final RestClient restClient;
	private final JsonMapper json = new JsonMapper();

	public NotificacionClient(RestClient externalRestClient) {
		this.restClient = externalRestClient;
	}

	public ResultadoEnvio enviar(String clave, NotificacionRequest request) {
		try {
			NotificacionResponse response = restClient.post()
					.uri("/api/notificaciones")
					.header("Idempotency-Key", clave)
					.body(request)
					.retrieve()
					.body(NotificacionResponse.class);
			if (response == null || !"ACEPTADA".equals(response.estado()) || response.idNotificacion() == null) {
				return new ResultadoEnvio(EstadoNotificacion.PENDIENTE_REINTENTO, null, "RESPUESTA_INVALIDA");
			}
			UUID.fromString(response.idNotificacion());
			return new ResultadoEnvio(EstadoNotificacion.ACEPTADA, response.idNotificacion(), null);
		} catch (RestClientResponseException error) {
			return interpretarError(error);
		} catch (RestClientException | IllegalArgumentException error) {
			return new ResultadoEnvio(EstadoNotificacion.PENDIENTE_REINTENTO, null, "API_NO_DISPONIBLE");
		}
	}

	private ResultadoEnvio interpretarError(RestClientResponseException error) {
		try {
			JsonNode body = json.readTree(error.getResponseBodyAsString());
			if ("ENVIO_INCIERTO".equals(body.path("codigo").asString(""))) {
				return new ResultadoEnvio(EstadoNotificacion.INCIERTA, null, "ENVIO_INCIERTO");
			}
			if (body.has("reintentable") && !body.path("reintentable").asBoolean()) {
				return new ResultadoEnvio(EstadoNotificacion.FALLIDA, null, "SMTP_RECHAZADO");
			}
		} catch (RuntimeException invalidBody) {
			// La clasificación HTTP también cubre errores de un intermediario sin JSON.
		}
		EstadoNotificacion estado = error.getStatusCode().is4xxClientError()
				? EstadoNotificacion.FALLIDA : EstadoNotificacion.PENDIENTE_REINTENTO;
		return new ResultadoEnvio(estado, null, "HTTP_" + error.getStatusCode().value());
	}

	public record ResultadoEnvio(EstadoNotificacion estado, String idExterno, String error) {
	}
}

