package cl.bello.petcare;

import static org.assertj.core.api.Assertions.assertThat;

import cl.bello.petcare.dto.NotificacionRequest;
import cl.bello.petcare.entity.EstadoNotificacion;
import cl.bello.petcare.service.NotificacionClient;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClient;

class NotificacionClientTests {
	@ParameterizedTest
	@CsvSource({
		"202, ACEPTADA, ACEPTADA", "202, INVALIDA, PENDIENTE_REINTENTO",
		"503, TEMPORAL, PENDIENTE_REINTENTO", "503, PERMANENTE, FALLIDA",
		"409, INCIERTA, INCIERTA", "409, CONFLICTO, FALLIDA", "400, INVALIDA, FALLIDA",
		"502, SIN_JSON, PENDIENTE_REINTENTO" })
	void interpretaRespuestaHttpYPropagaClaveEstable(int status, String caso, EstadoNotificacion esperado) throws Exception {
		String clave = UUID.randomUUID().toString();
		String externo = UUID.randomUUID().toString();
		AtomicReference<String> recibida = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/notificaciones", exchange -> {
			recibida.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
			exchange.getRequestBody().readAllBytes();
			String body = switch (caso) {
				case "ACEPTADA" -> "{\"estado\":\"ACEPTADA\",\"idNotificacion\":\"" + externo + "\"}";
				case "INCIERTA" -> "{\"codigo\":\"ENVIO_INCIERTO\",\"reintentable\":false}";
				case "PERMANENTE" -> "{\"codigo\":\"SMTP_RECHAZADO\",\"reintentable\":false}";
				case "TEMPORAL" -> "{\"reintentable\":true}";
				case "SIN_JSON" -> "upstream error";
				default -> "{}";
			};
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		try {
			var client = new NotificacionClient(RestClient.builder()
					.baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
			var resultado = client.enviar(clave,
					new NotificacionRequest("etapa4@example.invalid", "EMAIL", "Etapa4", "Mensaje", 1L));
			assertThat(resultado.estado()).isEqualTo(esperado);
			assertThat(recibida.get()).isEqualTo(clave);
			if (esperado == EstadoNotificacion.ACEPTADA) {
				assertThat(resultado.idExterno()).isEqualTo(externo);
			}
		} finally {
			server.stop(0);
		}
	}
}
