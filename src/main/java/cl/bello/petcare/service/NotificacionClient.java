package cl.bello.petcare.service;

import cl.bello.petcare.dto.NotificacionRequest;
import cl.bello.petcare.dto.NotificacionResponse;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class NotificacionClient {

	private final RestClient restClient;

	public NotificacionClient(RestClient externalRestClient) {
		this.restClient = externalRestClient;
	}

	public String enviarCambioReserva(Reserva reserva) {
		if (reserva.getEstado() != EstadoReserva.CONFIRMADA
				&& reserva.getEstado() != EstadoReserva.CANCELADA) {
			return "NO_APLICA";
		}
		String asunto = "Reserva " + reserva.getEstado().name().toLowerCase();
		String mensaje = "La reserva de " + reserva.getMascota().getNombre()
				+ " para " + reserva.getFechaHora() + " quedó "
				+ reserva.getEstado().name().toLowerCase() + ".";
		NotificacionRequest request = new NotificacionRequest(
				reserva.getMascota().getCliente().getEmail(),
				"EMAIL",
				asunto,
				mensaje,
				reserva.getId());
		try {
			NotificacionResponse response = restClient.post()
					.uri("/api/notificaciones")
					.body(request)
					.retrieve()
					.body(NotificacionResponse.class);
			if (response == null) {
				return "PENDIENTE_REINTENTO";
			}
			return response.estado() + ":" + response.idNotificacion();
		} catch (RestClientException error) {
			return "PENDIENTE_REINTENTO";
		}
	}
}

