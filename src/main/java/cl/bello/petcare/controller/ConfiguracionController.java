package cl.bello.petcare.controller;

import cl.bello.petcare.dto.ApiError;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfiguracionController {

	private static final String RUTA = "/api/configuracion";
	private final URI externalApiUrl;

	public ConfiguracionController(@Value("${petcare.external-api.base-url}") String baseUrl) {
		this.externalApiUrl = direccionPublica(baseUrl);
	}

	@GetMapping(RUTA)
	public ResponseEntity<?> obtener() {
		if (externalApiUrl == null) {
			HttpStatus estado = HttpStatus.SERVICE_UNAVAILABLE;
			ApiError error = new ApiError(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
					"La dirección de la API externa no está configurada correctamente.", RUTA, List.of());
			return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(error);
		}
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ConfiguracionPublica(externalApiUrl));
	}

	private static URI direccionPublica(String baseUrl) {
		try {
			URI uri = URI.create(baseUrl);
			boolean http = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
			boolean publica = uri.getHost() != null && uri.getRawUserInfo() == null
					&& uri.getRawQuery() == null && uri.getRawFragment() == null;
			return http && publica && uri.getPort() != 0 && uri.getPort() <= 65535 ? uri : null;
		} catch (IllegalArgumentException error) {
			return null;
		}
	}

	public record ConfiguracionPublica(URI externalApiUrl) {
	}
}
