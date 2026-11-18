package cl.bello.petcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.bello.petcare.config.NotificacionProperties;
import cl.bello.petcare.dto.EstadoReservaRequest;
import cl.bello.petcare.entity.Cliente;
import cl.bello.petcare.entity.EstadoNotificacion;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Mascota;
import cl.bello.petcare.entity.NotificacionPendiente;
import cl.bello.petcare.entity.Reserva;
import cl.bello.petcare.entity.Veterinario;
import cl.bello.petcare.repository.AtencionMedicaRepository;
import cl.bello.petcare.repository.ClienteRepository;
import cl.bello.petcare.repository.MascotaRepository;
import cl.bello.petcare.repository.NotificacionRepository;
import cl.bello.petcare.repository.ReservaRepository;
import cl.bello.petcare.repository.VeterinarioRepository;
import cl.bello.petcare.service.NotificacionClient;
import cl.bello.petcare.service.NotificacionClient.ResultadoEnvio;
import cl.bello.petcare.service.NotificacionDispatcher;
import cl.bello.petcare.service.ReservaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:petcare_etapa4;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
		"petcare.notifications.max-attempts=3", "petcare.notifications.retry-delay=1s",
		"petcare.notifications.max-retry-delay=4s", "petcare.notifications.lease-duration=5s" })
@Import(NotificacionesTests.Dependencias.class)
class NotificacionesTests {
	@Autowired NotificacionRepository eventos;
	@Autowired ReservaRepository reservas;
	@Autowired ClienteRepository clientes;
	@Autowired MascotaRepository mascotas;
	@Autowired VeterinarioRepository veterinarios;
	@Autowired AtencionMedicaRepository atenciones;
	@Autowired ReservaService servicio;
	@Autowired NotificacionDispatcher dispatcher;
	@Autowired NotificacionClient client;
	@Autowired Reloj reloj;
	@Autowired PlatformTransactionManager manager;
	@Autowired NotificacionProperties properties;
	private Reserva reserva;
	private TransactionTemplate transaction;

	@BeforeEach
	void preparar() {
		reset(client);
		reloj.ahora = Instant.parse("2026-11-17T12:00:00Z");
		eventos.deleteAll();
		atenciones.deleteAll();
		reservas.deleteAll();
		mascotas.deleteAll();
		veterinarios.deleteAll();
		clientes.deleteAll();
		Cliente cliente = clientes.saveAndFlush(new Cliente("Etapa4", "etapa4@example.invalid", "12345678"));
		Mascota mascota = mascotas.saveAndFlush(new Mascota(cliente, "Etapa4 Milo", "Gato", "Mestizo",
				LocalDate.of(2022, 1, 1), BigDecimal.TEN));
		Veterinario vet = veterinarios.saveAndFlush(new Veterinario("Etapa4 Vet", "General", "vet4@example.invalid"));
		reserva = reservas.saveAndFlush(new Reserva(mascota, vet, LocalDateTime.now().plusDays(2)));
		transaction = new TransactionTemplate(manager);
		when(client.enviar(any(), any())).thenReturn(new ResultadoEnvio(EstadoNotificacion.ACEPTADA,
				UUID.randomUUID().toString(), null));
	}

	@Test
	void confirmacionYCancelacionPersistenEventosDistintosSinEnviarDuranteLaTransaccion() {
		var confirmada = servicio.cambiarEstado(reserva.getId(), new EstadoReservaRequest(EstadoReserva.CONFIRMADA));
		assertThat(confirmada.resultadoNotificacion()).startsWith("PENDIENTE:");
		verifyNoInteractions(client);
		NotificacionPendiente primero = ultimo();
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.ACEPTADA);
		assertThat(servicio.obtener(reserva.getId()).resultadoNotificacion()).startsWith("ACEPTADA:");
		servicio.cambiarEstado(reserva.getId(), new EstadoReservaRequest(EstadoReserva.CANCELADA));
		assertThat(ultimo().getClaveEvento()).isNotEqualTo(primero.getClaveEvento());
		dispatcher.procesarPendientes();
		assertThat(eventos.count()).isEqualTo(2);
		verify(client, times(2)).enviar(any(), any());
	}

	@Test
	void repeticionDeTransicionNoCreaAvisos() {
		confirmar();
		var repetida = servicio.cambiarEstado(reserva.getId(), new EstadoReservaRequest(EstadoReserva.CONFIRMADA));
		assertThat(repetida.resultadoNotificacion()).isEqualTo("NO_APLICA");
		assertThat(eventos.count()).isOne();
		dispatcher.procesarPendientes();
		dispatcher.procesarPendientes();
		verify(client).enviar(any(), any());
	}

	@Test
	void rollbackNoDejaEventoNiPermiteEnvioAntesDelCommit() {
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
			confirmar();
			try (var executor = Executors.newSingleThreadExecutor()) {
				executor.submit(dispatcher::procesarPendientes).get(5, TimeUnit.SECONDS);
			} catch (Exception error) {
				throw new AssertionError(error);
			}
			verifyNoInteractions(client);
			throw new IllegalStateException("Rollback de prueba");
		})).isInstanceOf(IllegalStateException.class).hasMessage("Rollback de prueba");
		assertThat(eventos.count()).isZero();
		assertThat(reservas.findById(reserva.getId()).orElseThrow().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
		dispatcher.procesarPendientes();
		verifyNoInteractions(client);
	}

	@Test
	void caidaDeNodeConservaReservaYReintentaLaMismaClave() {
		when(client.enviar(any(), any())).thenReturn(reintento())
				.thenReturn(new ResultadoEnvio(EstadoNotificacion.ACEPTADA, UUID.randomUUID().toString(), null));
		confirmar();
		String clave = ultimo().getClaveEvento();
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_REINTENTO);
		assertThat(ultimo().getProximoIntento()).isEqualTo(reloj.instant().plusSeconds(1));
		assertThat(servicio.obtener(reserva.getId()).estado()).isEqualTo(EstadoReserva.CONFIRMADA);
		dispatcher.procesarPendientes();
		verify(client).enviar(eq(clave), any());
		reloj.avanzar(1);
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.ACEPTADA);
		assertThat(ultimo().getIntentos()).isEqualTo(2);
		verify(client, times(2)).enviar(eq(clave), any());
	}

	@Test
	void agotaTresIntentosConEsperaExponencialYDetieneElProcesamiento() {
		when(client.enviar(any(), any())).thenReturn(reintento());
		confirmar();
		dispatcher.procesarPendientes();
		reloj.avanzar(1);
		dispatcher.procesarPendientes();
		assertThat(ultimo().getProximoIntento()).isEqualTo(reloj.instant().plusSeconds(2));
		reloj.avanzar(2);
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
		assertThat(ultimo().getIntentos()).isEqualTo(3);
		assertThat(ultimo().getUltimoError()).isEqualTo("API_NO_DISPONIBLE");
		reloj.avanzar(100);
		dispatcher.procesarPendientes();
		verify(client, times(3)).enviar(any(), any());
	}

	@Test
	void resultadoInciertoDetieneReintentosParaRevision() {
		comprobarFinal(EstadoNotificacion.INCIERTA, "ENVIO_INCIERTO");
	}

	@Test
	void rechazoPermanenteDetieneReintentos() {
		comprobarFinal(EstadoNotificacion.FALLIDA, "SMTP_RECHAZADO");
	}

	@Test
	void nuevoTrabajadorRecuperaAvisoPersistidoConConcesionVencida() {
		confirmar();
		Long id = ultimo().getId();
		transaction.executeWithoutResult(status -> eventos.buscarParaActualizar(id).orElseThrow()
				.iniciarIntento(reloj.instant(), Duration.ofSeconds(5)));
		NotificacionDispatcher nuevo = new NotificacionDispatcher(eventos, client, properties, reloj, manager);
		nuevo.procesarPendientes();
		verifyNoInteractions(client);
		reloj.avanzar(5);
		nuevo.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.ACEPTADA);
		assertThat(ultimo().getIntentos()).isEqualTo(2);
	}

	@Test
	void ultimoIntentoInterrumpidoSeConservaComoIncierto() {
		confirmar();
		Long id = ultimo().getId();
		transaction.executeWithoutResult(status -> {
			var evento = eventos.buscarParaActualizar(id).orElseThrow();
			for (int intento = 0; intento < properties.maxAttempts(); intento++) {
				evento.iniciarIntento(reloj.instant(), Duration.ofSeconds(5));
			}
		});
		reloj.avanzar(5);
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.INCIERTA);
		assertThat(ultimo().getUltimoError()).isEqualTo("INTENTO_INTERRUMPIDO");
		verifyNoInteractions(client);
	}

	@Test
	void trabajadoresConcurrentesCoordinanAvisoYEnvianFueraDeTransaccion() throws Exception {
		confirmar();
		Long id = ultimo().getId();
		CountDownLatch enviando = new CountDownLatch(1);
		CountDownLatch continuar = new CountDownLatch(1);
		when(client.enviar(any(), any())).thenAnswer(invocation -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			enviando.countDown();
			assertThat(continuar.await(5, TimeUnit.SECONDS)).isTrue();
			return new ResultadoEnvio(EstadoNotificacion.ACEPTADA, UUID.randomUUID().toString(), null);
		});
		try (var executor = Executors.newFixedThreadPool(2)) {
			var primero = executor.submit(() -> dispatcher.procesar(id));
			assertThat(enviando.await(5, TimeUnit.SECONDS)).isTrue();
			try {
				executor.submit(() -> dispatcher.procesar(id)).get(5, TimeUnit.SECONDS);
				assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
			} finally {
				continuar.countDown();
			}
			primero.get(5, TimeUnit.SECONDS);
		}
		verify(client).enviar(any(), any());
		assertThat(ultimo().getEstado()).isEqualTo(EstadoNotificacion.ACEPTADA);
	}

	private void comprobarFinal(EstadoNotificacion estado, String error) {
		when(client.enviar(any(), any())).thenReturn(new ResultadoEnvio(estado, null, error));
		confirmar();
		dispatcher.procesarPendientes();
		reloj.avanzar(100);
		dispatcher.procesarPendientes();
		assertThat(ultimo().getEstado()).isEqualTo(estado);
		verify(client).enviar(any(), any());
	}

	private void confirmar() {
		servicio.cambiarEstado(reserva.getId(), new EstadoReservaRequest(EstadoReserva.CONFIRMADA));
	}

	private NotificacionPendiente ultimo() {
		return eventos.findFirstByReservaIdOrderByIdDesc(reserva.getId()).orElseThrow();
	}

	private ResultadoEnvio reintento() {
		return new ResultadoEnvio(EstadoNotificacion.PENDIENTE_REINTENTO, null, "API_NO_DISPONIBLE");
	}

	@TestConfiguration
	static class Dependencias {
		@Bean @Primary Reloj relojPrueba() { return new Reloj(); }
		@Bean @Primary NotificacionClient clientePrueba() { return mock(NotificacionClient.class); }
	}

	static class Reloj extends Clock {
		volatile Instant ahora;
		void avanzar(long segundos) { ahora = ahora.plusSeconds(segundos); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return Clock.fixed(ahora, zone); }
		@Override public Instant instant() { return ahora; }
	}
}
