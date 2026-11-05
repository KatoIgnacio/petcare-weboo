package cl.bello.petcare;

import static org.assertj.core.api.Assertions.assertThat;

import cl.bello.petcare.controller.ClienteController;
import cl.bello.petcare.controller.MascotaController;
import cl.bello.petcare.controller.ReservaController;
import cl.bello.petcare.controller.VeterinarioController;
import cl.bello.petcare.dto.AtencionRequest;
import cl.bello.petcare.dto.AtencionResponse;
import cl.bello.petcare.dto.ClienteRequest;
import cl.bello.petcare.dto.ClienteResponse;
import cl.bello.petcare.dto.EstadoReservaRequest;
import cl.bello.petcare.dto.MascotaRequest;
import cl.bello.petcare.dto.MascotaResponse;
import cl.bello.petcare.dto.ReservaRequest;
import cl.bello.petcare.dto.ReservaResponse;
import cl.bello.petcare.dto.VeterinarioRequest;
import cl.bello.petcare.dto.VeterinarioResponse;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.repository.AtencionMedicaRepository;
import cl.bello.petcare.repository.ClienteRepository;
import cl.bello.petcare.repository.MascotaRepository;
import cl.bello.petcare.repository.ReservaRepository;
import cl.bello.petcare.repository.VeterinarioRepository;
import cl.bello.petcare.service.ClienteService;
import cl.bello.petcare.service.MascotaService;
import cl.bello.petcare.service.ReservaService;
import cl.bello.petcare.service.VeterinarioService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetcareApplicationTests {

	private static final Pattern ID_JSON = Pattern.compile("\\\"id\\\":(\\d+)");

	@LocalServerPort
	private int puerto;

	@Autowired
	private ClienteController clienteController;

	@Autowired
	private MascotaController mascotaController;

	@Autowired
	private VeterinarioController veterinarioController;

	@Autowired
	private ReservaController reservaController;

	@Autowired
	private ClienteService clienteService;

	@Autowired
	private MascotaService mascotaService;

	@Autowired
	private VeterinarioService veterinarioService;

	@Autowired
	private ReservaService reservaService;

	@Autowired
	private AtencionMedicaRepository atencionRepository;

	@Autowired
	private ReservaRepository reservaRepository;

	@Autowired
	private MascotaRepository mascotaRepository;

	@Autowired
	private VeterinarioRepository veterinarioRepository;

	@Autowired
	private ClienteRepository clienteRepository;

	@BeforeEach
	void limpiarBase() {
		atencionRepository.deleteAll();
		reservaRepository.deleteAll();
		mascotaRepository.deleteAll();
		veterinarioRepository.deleteAll();
		clienteRepository.deleteAll();
	}

	@Test
	void cargaContextoYControladores() {
		assertThat(clienteController).isNotNull();
		assertThat(mascotaController).isNotNull();
		assertThat(veterinarioController).isNotNull();
		assertThat(reservaController).isNotNull();
	}

	@Test
	void endpointsCreanReservaYRechazanHorarioDuplicado() throws Exception {
		long clienteId = extraerId(post("/api/clientes", """
				{
				  "nombre": "Ana Pérez",
				  "email": "ana@ejemplo.cl",
				  "telefono": "+56911111111"
				}
				""").body());

		long mascotaId = extraerId(post("/api/clientes/" + clienteId + "/mascotas", """
				{
				  "nombre": "Luna",
				  "especie": "Perro",
				  "raza": "Mestiza",
				  "fechaNacimiento": "2021-04-10",
				  "peso": 12.5
				}
				""").body());

		long veterinarioId = extraerId(post("/api/veterinarios", """
				{
				  "nombre": "Dr. Carlos Díaz",
				  "especialidad": "Medicina general",
				  "email": "carlos@petcare.cl"
				}
				""").body());

		String fecha = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0).toString();
		String reservaJson = """
				{
				  "mascotaId": %d,
				  "veterinarioId": %d,
				  "fechaHora": "%s"
				}
				""".formatted(mascotaId, veterinarioId, fecha);

		HttpResponse<String> creada = post("/api/reservas", reservaJson);
		assertThat(creada.statusCode()).isEqualTo(201);
		assertThat(creada.body()).contains("PENDIENTE", "Luna");

		HttpResponse<String> duplicada = post("/api/reservas", reservaJson);
		assertThat(duplicada.statusCode()).isEqualTo(409);
		assertThat(duplicada.body()).contains("horario");
	}

	@Test
	void reservaConfirmadaRegistraAtencionEHistorial() {
		ClienteResponse cliente = clienteService.crear(new ClienteRequest(
				"Cliente prueba", "cliente@ejemplo.cl", "+56922222222"));
		MascotaResponse mascota = mascotaService.crear(cliente.id(), new MascotaRequest(
				"Milo",
				"Gato",
				"Doméstico",
				LocalDate.of(2022, 2, 2),
				new BigDecimal("4.50")));
		VeterinarioResponse veterinario = veterinarioService.crear(new VeterinarioRequest(
				"Dra. Elena Soto", "Felinos", "elena@petcare.cl"));

		ReservaResponse reserva = reservaService.crear(new ReservaRequest(
				mascota.id(), veterinario.id(), LocalDateTime.now().plusDays(3)));
		ReservaResponse confirmada = reservaService.cambiarEstado(
				reserva.id(), new EstadoReservaRequest(EstadoReserva.CONFIRMADA));
		assertThat(confirmada.estado()).isEqualTo(EstadoReserva.CONFIRMADA);

		AtencionResponse atencion = reservaService.registrarAtencion(
				reserva.id(),
				new AtencionRequest(
						"Dermatitis leve",
						"Tratamiento tópico por siete días",
						List.of("Clorhexidina"),
						"Control en una semana"));

		assertThat(atencion.id()).isNotNull();
		assertThat(reservaService.obtener(reserva.id()).estado()).isEqualTo(EstadoReserva.ATENDIDA);
		assertThat(reservaService.obtener(reserva.id()).atencion()).isNotNull();
	}

	private HttpResponse<String> post(String ruta, String json) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + puerto + ruta))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	private long extraerId(String json) {
		Matcher matcher = ID_JSON.matcher(json);
		if (!matcher.find()) {
			throw new AssertionError("La respuesta no contiene un id: " + json);
		}
		return Long.parseLong(matcher.group(1));
	}
}

