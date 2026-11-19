package cl.bello.petcare;

import static org.assertj.core.api.Assertions.assertThat;

import cl.bello.petcare.entity.AtencionMedica;
import cl.bello.petcare.entity.Cliente;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Mascota;
import cl.bello.petcare.entity.Reserva;
import cl.bello.petcare.entity.Veterinario;
import cl.bello.petcare.repository.AtencionMedicaRepository;
import cl.bello.petcare.repository.ClienteRepository;
import cl.bello.petcare.repository.MascotaRepository;
import cl.bello.petcare.repository.NotificacionRepository;
import cl.bello.petcare.repository.ReservaRepository;
import cl.bello.petcare.repository.VeterinarioRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.datasource.url=jdbc:h2:mem:petcare_etapa3;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@Import(BackendEndpointsTests.ErrorDePruebaController.class)
class BackendEndpointsTests {

	private final HttpClient http = HttpClient.newHttpClient();
	private final JsonMapper json = new JsonMapper();

	@LocalServerPort
	private int puerto;

	@Autowired
	private ClienteRepository clientes;
	@Autowired
	private MascotaRepository mascotas;
	@Autowired
	private VeterinarioRepository veterinarios;
	@Autowired
	private ReservaRepository reservas;
	@Autowired
	private AtencionMedicaRepository atenciones;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private NotificacionRepository notificaciones;

	@BeforeEach
	void limpiarBaseDePruebas() {
		notificaciones.deleteAll();
		atenciones.deleteAll();
		reservas.deleteAll();
		mascotas.deleteAll();
		veterinarios.deleteAll();
		clientes.deleteAll();
	}

	@Test
	void clientesActivosOrdenadosPorNombreEId() throws Exception {
		Cliente z = cliente("Zoe", "zoe");
		Cliente ana1 = cliente("Ana", "ana1");
		Cliente ana2 = cliente("Ana", "ana2");
		Cliente baja = cliente("Aaron", "baja");
		request("DELETE", "/api/clientes/" + baja.getId(), 204, null);

		JsonNode lista = request("GET", "/api/clientes", 200, null);
		assertThat(ids(lista)).containsExactly(ana1.getId(), ana2.getId(), z.getId());
		assertThat(lista.get(0).get("email").asString()).isEqualTo(ana1.getEmail());
		assertThat(lista.get(0).get("activo").asBoolean()).isTrue();
	}

	@Test
	void listadosVaciosDevuelvenArreglos() throws Exception {
		assertThat(request("GET", "/api/clientes", 200, null).isEmpty()).isTrue();
		assertThat(request("GET", "/api/reservas", 200, null).isEmpty()).isTrue();
	}

	@Test
	void agendaFiltraPorDiaYVeterinarioConLimitesYOrdenEstable() throws Exception {
		Cliente cliente = cliente("Etapa3 Agenda", "agenda");
		Mascota mascota = mascota(cliente, "Agenda");
		Veterinario vet1 = veterinario("uno");
		Veterinario vet2 = veterinario("dos");
		LocalDate dia = LocalDate.of(2024, 5, 10);
		Reserva anterior = reserva(mascota, vet1, dia.atStartOfDay().minusNanos(1000), EstadoReserva.PENDIENTE);
		Reserva inicio = reserva(mascota, vet1, dia.atStartOfDay(), EstadoReserva.CONFIRMADA);
		Reserva medio1 = reserva(mascota, vet1, dia.atTime(12, 0), EstadoReserva.CANCELADA);
		Reserva medio2 = reserva(mascota, vet2, dia.atTime(12, 0), EstadoReserva.ATENDIDA);
		Reserva fin = reserva(mascota, vet1, dia.plusDays(1).atStartOfDay().minusNanos(1000), EstadoReserva.PENDIENTE);
		Reserva siguiente = reserva(mascota, vet1, dia.plusDays(1).atStartOfDay(), EstadoReserva.PENDIENTE);

		JsonNode todas = request("GET", "/api/reservas", 200, null);
		assertThat(ids(todas)).containsExactly(anterior.getId(), inicio.getId(), medio1.getId(),
				medio2.getId(), fin.getId(), siguiente.getId());
		assertThat(ids(request("GET", "/api/reservas?fecha=" + dia, 200, null)))
				.containsExactly(inicio.getId(), medio1.getId(), medio2.getId(), fin.getId());
		assertThat(ids(request("GET", "/api/reservas?veterinarioId=" + vet2.getId(), 200, null)))
				.containsExactly(medio2.getId());
		JsonNode ambos = request("GET", "/api/reservas?fecha=" + dia + "&veterinarioId=" + vet1.getId(), 200, null);
		assertThat(ids(ambos)).containsExactly(inicio.getId(), medio1.getId(), fin.getId());
		assertThat(ambos.get(0).get("clienteNombre").asString()).isEqualTo(cliente.getNombre());
		assertThat(ambos.get(0).get("mascotaId").asLong()).isEqualTo(mascota.getId());
		assertThat(request("GET", "/api/reservas?fecha=2024-05-01", 200, null).isEmpty()).isTrue();
		assertThat(request("GET", "/api/reservas?veterinarioId=9223372036854775806", 200, null).isEmpty()).isTrue();

		request("DELETE", "/api/clientes/" + cliente.getId(), 204, null);
		assertThat(ids(request("GET", "/api/reservas?fecha=" + dia, 200, null)))
				.containsExactly(inicio.getId(), medio1.getId(), medio2.getId(), fin.getId());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"fecha=", "fecha=%20", "fecha=2024-02-30", "fecha=2024-2-01", "fecha=abc",
			"fecha=2024-05-10T10:00:00", "veterinarioId=", "veterinarioId=%20",
			"veterinarioId=0", "veterinarioId=-1", "veterinarioId=abc", "veterinarioId=1.5",
			"veterinarioId=9223372036854775808"
	})
	void filtrosInvalidosDevuelven400ConApiError(String query) throws Exception {
		JsonNode error = request("GET", "/api/reservas?" + query, 400, null);
		assertApiError(error, 400, "/api/reservas");
	}

	@Test
	void historialOrdenaFechaEIdConservaMedicamentosYBajasLogicas() throws Exception {
		Cliente cliente = cliente("Etapa3 Historial", "historial");
		Mascota mascota1 = mascota(cliente, "Uno");
		Mascota mascota2 = mascota(cliente, "Dos");
		Veterinario vet = veterinario("historial");
		LocalDateTime dia = LocalDateTime.of(2024, 5, 10, 10, 0);
		AtencionMedica antigua = atencion(reserva(mascota1, vet, dia, EstadoReserva.ATENDIDA), "Antigua");
		AtencionMedica reciente1 = atencion(reserva(mascota1, vet, dia.plusDays(1), EstadoReserva.ATENDIDA), "Reciente1");
		AtencionMedica reciente2 = atencion(reserva(mascota1, vet, dia.plusDays(2), EstadoReserva.ATENDIDA), "Reciente2");
		atencion(reserva(mascota2, vet, dia.plusDays(3), EstadoReserva.ATENDIDA), "Otra mascota");
		jdbc.update("update atenciones_medicas set fecha = ? where id = ?", dia, antigua.getId());
		jdbc.update("update atenciones_medicas set fecha = ? where id in (?, ?)",
				dia.plusDays(1), reciente1.getId(), reciente2.getId());

		String ruta = "/api/mascotas/" + mascota1.getId() + "/historial";
		JsonNode historial = request("GET", ruta, 200, null);
		assertThat(ids(historial)).containsExactly(reciente2.getId(), reciente1.getId(), antigua.getId());
		assertThat(historial.get(0).get("medicamentos").get(0).asString()).isEqualTo("Medicamento de prueba");
		assertThat(historial.get(0).get("observaciones").isNull()).isTrue();

		request("DELETE", "/api/mascotas/" + mascota1.getId(), 204, null);
		assertThat(request("GET", ruta, 200, null)).isEqualTo(historial);
		request("DELETE", "/api/clientes/" + cliente.getId(), 204, null);
		assertThat(request("GET", ruta, 200, null)).isEqualTo(historial);
		assertThat(request("GET", "/api/mascotas/" + mascota2.getId() + "/historial", 200, null).size()).isEqualTo(1);
	}

	@Test
	void historialVacioEInexistente() throws Exception {
		Mascota mascota = mascota(cliente("Etapa3 Vacio", "vacio"), "Vacia");
		String ruta = "/api/mascotas/" + mascota.getId() + "/historial";
		assertThat(request("GET", ruta, 200, null).isEmpty()).isTrue();
		request("DELETE", "/api/mascotas/" + mascota.getId(), 204, null);
		assertThat(request("GET", ruta, 200, null).isEmpty()).isTrue();
		assertApiError(request("GET", "/api/mascotas/9223372036854775806/historial", 404, null),
				404, "/api/mascotas/9223372036854775806/historial");
	}

	@Test
	void erroresHttpConservanApiErrorYCabeceras() throws Exception {
		assertApiError(request("GET", "/api/ruta-inexistente", 404, null), 404, "/api/ruta-inexistente");
		assertApiError(request("GET", "/api/clientes/abc", 400, null), 400, "/api/clientes/abc");
		HttpResponse<String> method = send("PUT", "/api/clientes", "{}", "application/json");
		assertThat(method.statusCode()).isEqualTo(405);
		assertThat(method.headers().firstValue("Allow").orElseThrow()).contains("GET", "POST");
		assertApiError(json.readTree(method.body()), 405, "/api/clientes");
		HttpResponse<String> media = send("POST", "/api/clientes", "texto", "text/plain");
		assertThat(media.statusCode()).isEqualTo(415);
		assertApiError(json.readTree(media.body()), 415, "/api/clientes");
		HttpResponse<String> invalidJson = send("POST", "/api/clientes", "{", "application/json");
		assertThat(invalidJson.statusCode()).isEqualTo(400);
		assertApiError(json.readTree(invalidJson.body()), 400, "/api/clientes");
		JsonNode invalid = request("POST", "/api/clientes", 400,
				Map.of("nombre", "", "email", "invalido", "telefono", ""));
		assertThat(invalid.get("detalles").isEmpty()).isFalse();
	}

	@Test
	void raizSirveLaInterfazDePetCare() throws Exception {
		HttpResponse<String> raiz = send("GET", "/", null, "text/html");
		assertThat(raiz.statusCode()).isEqualTo(200);
		assertThat(raiz.headers().firstValue("Content-Type").orElseThrow()).contains("text/html");
		assertThat(raiz.body()).contains("<title>PetCare</title>", "/js/app.js", "/css/petcare.css");
		assertThat(send("GET", "/js/app.js", null, "text/javascript").statusCode()).isEqualTo(200);
		assertThat(send("GET", "/css/petcare.css", null, "text/css").statusCode()).isEqualTo(200);
	}

	@Test
	void configuracionPublicaExponeSoloLaBaseDeNode() throws Exception {
		HttpResponse<String> response = send("GET", "/api/configuracion", null, "application/json");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
		JsonNode configuracion = json.readTree(response.body());
		assertThat(configuracion.size()).isEqualTo(1);
		assertThat(configuracion.get("externalApiUrl").asString()).isEqualTo("http://localhost:65535");
	}

	@Test
	void falloInesperadoDevuelve500SinDetalleInterno() throws Exception {
		JsonNode error = request("GET", "/__test/error", 500, null);
		assertApiError(error, 500, "/__test/error");
		assertThat(error.toString()).doesNotContain("detalle interno de prueba", "IllegalStateException");
	}

	@Test
	void clienteInactivoImpideCrearYReprogramarAunqueLaMascotaSigaActiva() throws Exception {
		Cliente cliente = cliente("Etapa3 Inactivo", "inactivo");
		Mascota mascota = mascota(cliente, "Activa");
		Veterinario vet = veterinario("inactivo");
		LocalDateTime hora = diaFuturo().atTime(10, 0);
		long reservaId = request("POST", "/api/reservas", 201, datosReserva(mascota, vet, hora))
				.get("id").asLong();
		cliente.setActivo(false);
		clientes.saveAndFlush(cliente);
		assertApiError(request("POST", "/api/reservas", 404,
				datosReserva(mascota, vet, hora.plusHours(1))), 404, "/api/reservas");
		assertApiError(request("PUT", "/api/reservas/" + reservaId, 404,
				Map.of("veterinarioId", vet.getId(), "fechaHora", hora.plusHours(1).toString())),
				404, "/api/reservas/" + reservaId);
	}

	@Test
	void reservaValidaEstadosDisponibilidadYAtencion() throws Exception {
		Cliente cliente = cliente("Etapa3 Reglas", "reglas");
		Mascota mascota1 = mascota(cliente, "Uno");
		Mascota mascota2 = mascota(cliente, "Dos");
		Veterinario vet1 = veterinario("reglas1");
		Veterinario vet2 = veterinario("reglas2");
		LocalDateTime hora = diaFuturo().atTime(10, 0);
		long id = request("POST", "/api/reservas", 201, datosReserva(mascota1, vet1, hora)).get("id").asLong();
		request("POST", "/api/reservas", 409, datosReserva(mascota2, vet1, hora));
		request("POST", "/api/reservas", 409, datosReserva(mascota1, vet2, hora));
		request("POST", "/api/reservas", 400, datosReserva(mascota1, vet1, hora.minusYears(10)));
		request("PUT", "/api/reservas/" + id, 200,
				Map.of("veterinarioId", vet1.getId(), "fechaHora", hora.toString()));
		Map<String, Object> datosAtencion = Map.of("diagnostico", "Etapa3 prueba", "tratamiento", "Prueba",
				"medicamentos", List.of("Medicamento de prueba"));
		request("POST", "/api/reservas/" + id + "/atencion", 409, datosAtencion);
		request("PATCH", "/api/reservas/" + id + "/estado", 409, Map.of("estado", "ATENDIDA"));
		request("PATCH", "/api/reservas/" + id + "/estado", 200, Map.of("estado", "CONFIRMADA"));
		request("POST", "/api/reservas/" + id + "/atencion", 201, datosAtencion);
		assertThat(request("GET", "/api/reservas/" + id, 200, null).get("estado").asString()).isEqualTo("ATENDIDA");
		request("POST", "/api/reservas/" + id + "/atencion", 409, datosAtencion);
		request("PUT", "/api/reservas/" + id, 409,
				Map.of("veterinarioId", vet1.getId(), "fechaHora", hora.plusHours(1).toString()));
		request("PATCH", "/api/reservas/" + id + "/estado", 409, Map.of("estado", "CANCELADA"));
	}

	@Test
	void cancelacionLiberaHorarioYRestringeCambios() throws Exception {
		Mascota mascota = mascota(cliente("Etapa3 Cancelacion", "cancelacion"), "Cancelacion");
		Veterinario vet = veterinario("cancelacion");
		LocalDateTime hora = diaFuturo().atTime(10, 0);
		long id = request("POST", "/api/reservas", 201, datosReserva(mascota, vet, hora)).get("id").asLong();
		request("PATCH", "/api/reservas/" + id + "/estado", 200, Map.of("estado", "CONFIRMADA"));
		request("PATCH", "/api/reservas/" + id + "/estado", 200, Map.of("estado", "CANCELADA"));
		request("PATCH", "/api/reservas/" + id + "/estado", 409, Map.of("estado", "CONFIRMADA"));
		request("PUT", "/api/reservas/" + id, 409,
				Map.of("veterinarioId", vet.getId(), "fechaHora", hora.plusHours(1).toString()));
		request("POST", "/api/reservas", 201, datosReserva(mascota, vet, hora));
		jdbc.update("update veterinarios set activo = false where id = ?", vet.getId());
		request("POST", "/api/reservas", 404, datosReserva(mascota, vet, hora.plusHours(2)));
		request("DELETE", "/api/mascotas/" + mascota.getId(), 204, null);
		request("POST", "/api/reservas", 404, datosReserva(mascota, vet, hora.plusHours(3)));
	}

	@Test
	void disponibilidadOrdenadaRespetaHorarioYRechazaHorasFueraDeBloque() throws Exception {
		Mascota paciente = mascota(cliente("Disponibilidad", "limites"), "Luna");
		Veterinario vet = veterinario("limites");
		LocalDate dia = diaFuturo();
		List<String> libres = horas(vet, dia, "");
		assertThat(libres).hasSize(21).isSorted().startsWith("08:30").endsWith("18:30");
		request("POST", "/api/reservas", 201, datosReserva(paciente, vet, dia.atTime(8, 30)));
		request("POST", "/api/reservas", 201, datosReserva(paciente, vet, dia.atTime(18, 30)));
		assertThat(horas(vet, dia, "")).hasSize(19).doesNotContain("08:30", "18:30");
		for (String valor : List.of("08:00", "09:15", "10:00:01", "19:00")) {
			assertThat(request("POST", "/api/reservas", 400,
					datosReserva(paciente, vet, dia.atTime(LocalTime.parse(valor))))
					.get("mensaje").asString()).contains("bloque de 30 minutos");
		}
		assertThat(horas(vet, dia.minusYears(1), "")).isEmpty();
		String ruta = "/api/veterinarios/" + vet.getId() + "/disponibilidad";
		assertApiError(request("GET", ruta + "?fecha=2026-02-30", 400, null), 400, ruta);
		assertApiError(request("GET", ruta + "?fecha=2026-2-01", 400, null), 400, ruta);
		assertApiError(request("GET", ruta, 400, null), 400, ruta);
		String inexistente = "/api/veterinarios/9223372036854775806/disponibilidad";
		assertApiError(request("GET", inexistente + "?fecha=" + dia, 404, null), 404, inexistente);
	}

	@Test
	void citasAntiguasBloqueanBloquesSolapadosYLaCancelacionLosLibera() throws Exception {
		Cliente cliente = cliente("Disponibilidad", "solapamiento");
		Mascota paciente = mascota(cliente, "Luna");
		Mascota otro = mascota(cliente, "Sol");
		Veterinario vet = veterinario("solapamiento");
		Veterinario otroVet = veterinario("solapamiento2");
		LocalDate dia = diaFuturo();
		Reserva antigua = reserva(paciente, vet, dia.atTime(10, 15), EstadoReserva.PENDIENTE);
		String ruta = "/api/reservas/" + antigua.getId();
		assertThat(horas(vet, dia, "")).contains("09:30", "11:00").doesNotContain("10:00", "10:30");
		assertThat(horas(otroVet, dia, "")).contains("10:00", "10:30");
		assertThat(request("POST", "/api/reservas", 409, datosReserva(otro, vet, dia.atTime(10, 0)))
				.get("mensaje").asString()).contains("veterinario");
		assertThat(request("POST", "/api/reservas", 409, datosReserva(paciente, otroVet, dia.atTime(10, 30)))
				.get("mensaje").asString()).contains("mascota");
		request("PATCH", ruta + "/estado", 200, Map.of("estado", "CONFIRMADA"));
		assertThat(horas(vet, dia, "")).doesNotContain("10:30");
		request("PATCH", ruta + "/estado", 200, Map.of("estado", "CANCELADA"));
		assertThat(horas(vet, dia, "")).contains("10:00", "10:30");
		assertThat(request("GET", ruta, 200, null).get("fechaHora").asString()).startsWith(dia + "T10:15");
	}

	@Test
	void reprogramacionSoloExcluyeLaReservaPropiaDelProfesionalConsultado() throws Exception {
		Cliente cliente = cliente("Disponibilidad", "reprogramacion");
		Mascota paciente = mascota(cliente, "Luna");
		Mascota otro = mascota(cliente, "Sol");
		Veterinario vet = veterinario("reprogramacion");
		Veterinario otroVet = veterinario("reprogramacion2");
		LocalDate dia = diaFuturo();
		long id = request("POST", "/api/reservas", 201, datosReserva(paciente, vet, dia.atTime(10, 0)))
				.get("id").asLong();
		request("POST", "/api/reservas", 201, datosReserva(otro, otroVet, dia.atTime(10, 0)));
		assertThat(horas(vet, dia, "&reservaId=" + id)).contains("10:00");
		assertThat(horas(otroVet, dia, "&reservaId=" + id)).doesNotContain("10:00");
		String ruta = "/api/reservas/" + id;
		request("PUT", ruta, 400, Map.of("veterinarioId", vet.getId(), "fechaHora", dia + "T10:15"));
		request("PUT", ruta, 409, Map.of("veterinarioId", otroVet.getId(), "fechaHora", dia + "T10:00"));
		request("PUT", ruta, 200, Map.of("veterinarioId", vet.getId(), "fechaHora", dia + "T10:30"));
		assertThat(horas(vet, dia, "")).contains("10:00").doesNotContain("10:30");
	}

	@Test
	void dosSolicitudesConcurrentesPorElMismoCupoSoloCreanUnaReserva() throws Exception {
		Cliente cliente = cliente("Disponibilidad", "concurrencia");
		Veterinario vet = veterinario("concurrencia");
		LocalDateTime hora = diaFuturo().atTime(10, 0);
		List<CompletableFuture<HttpResponse<String>>> respuestas = new ArrayList<>();
		for (Mascota paciente : List.of(mascota(cliente, "Luna"), mascota(cliente, "Sol"))) {
			String body = json.writeValueAsString(datosReserva(paciente, vet, hora));
			HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/reservas"))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
			respuestas.add(http.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString()));
		}
		CompletableFuture.allOf(respuestas.toArray(CompletableFuture<?>[]::new)).get(15, TimeUnit.SECONDS);
		assertThat(respuestas.stream().map(r -> r.join().statusCode()).toList()).containsExactlyInAnyOrder(201, 409);
		assertThat(reservas.count()).isEqualTo(1L);
	}

	private LocalDate diaFuturo() {
		return LocalDate.now(ZoneId.of("America/Santiago")).plusDays(4);
	}

	private List<String> horas(Veterinario vet, LocalDate dia, String extra) throws Exception {
		List<String> horas = new ArrayList<>();
		request("GET", "/api/veterinarios/" + vet.getId() + "/disponibilidad?fecha=" + dia + extra, 200, null)
				.forEach(hora -> horas.add(hora.asString()));
		return horas;
	}

	private Cliente cliente(String nombre, String clave) {
		return clientes.saveAndFlush(new Cliente(nombre, "etapa3." + clave + "@example.invalid", "+56911112222"));
	}

	private Mascota mascota(Cliente cliente, String nombre) {
		return mascotas.saveAndFlush(new Mascota(cliente, nombre, "Canina", "Mestiza",
				LocalDate.of(2022, 1, 1), new BigDecimal("8.50")));
	}

	private Veterinario veterinario(String clave) {
		return veterinarios.saveAndFlush(new Veterinario("Etapa3 " + clave, "General",
				"etapa3.vet." + clave + "@example.invalid"));
	}

	private Reserva reserva(Mascota mascota, Veterinario vet, LocalDateTime fecha, EstadoReserva estado) {
		Reserva reserva = new Reserva(mascota, vet, fecha);
		reserva.setEstado(estado);
		return reservas.saveAndFlush(reserva);
	}

	private AtencionMedica atencion(Reserva reserva, String diagnostico) {
		return atenciones.saveAndFlush(new AtencionMedica(reserva, diagnostico, "Tratamiento de prueba",
				List.of("Medicamento de prueba"), null));
	}

	private Map<String, Object> datosReserva(Mascota mascota, Veterinario vet, LocalDateTime hora) {
		return Map.of("mascotaId", mascota.getId(), "veterinarioId", vet.getId(), "fechaHora", hora.toString());
	}

	private JsonNode request(String metodo, String ruta, int esperado, Object body) throws Exception {
		HttpResponse<String> response = send(metodo, ruta,
				body == null ? null : json.writeValueAsString(body), "application/json");
		assertThat(response.statusCode()).as("%s %s -> %s", metodo, ruta, response.body()).isEqualTo(esperado);
		return response.body().isBlank() ? null : json.readTree(response.body());
	}

	private HttpResponse<String> send(String metodo, String ruta, String body, String contentType) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
				.header("Content-Type", contentType)
				.method(metodo, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
				.build();
		return http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private List<Long> ids(JsonNode lista) {
		List<Long> ids = new ArrayList<>();
		lista.forEach(item -> ids.add(item.get("id").asLong()));
		return ids;
	}

	private void assertApiError(JsonNode error, int status, String ruta) {
		assertThat(error.get("status").asInt()).isEqualTo(status);
		assertThat(error.get("ruta").asString()).isEqualTo(ruta);
		assertThat(error.get("timestamp").asString()).isNotBlank();
		assertThat(error.get("error").asString()).isNotBlank();
		assertThat(error.get("mensaje").asString()).isNotBlank();
		assertThat(error.get("detalles").isArray()).isTrue();
	}

	@RestController
	static class ErrorDePruebaController {
		@GetMapping("/__test/error")
		public String error() {
			throw new IllegalStateException("detalle interno de prueba");
		}
	}
}
