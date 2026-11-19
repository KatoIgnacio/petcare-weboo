package cl.bello.petcare.service;

import cl.bello.petcare.dto.MedicamentoResponse;
import cl.bello.petcare.dto.UbicacionClinicaResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class InformacionExternaClient {

	private final RestClient restClient;

	public InformacionExternaClient(RestClient externalRestClient) {
		this.restClient = externalRestClient;
	}

	public List<MedicamentoResponse> buscarMedicamentos(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			return List.of();
		}
		try {
			MedicamentoResponse[] respuesta = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/api/medicamentos")
							.queryParam("nombre", nombre.trim())
							.build())
					.retrieve()
					.body(MedicamentoResponse[].class);
			return respuesta == null ? List.of() : Arrays.asList(respuesta);
		} catch (RestClientException error) {
			return List.of();
		}
	}

	public Optional<UbicacionClinicaResponse> obtenerUbicacion() {
		try {
			return Optional.ofNullable(restClient.get()
					.uri("/api/ubicacion-clinica")
					.retrieve()
					.body(UbicacionClinicaResponse.class));
		} catch (RestClientException error) {
			return Optional.empty();
		}
	}
}
