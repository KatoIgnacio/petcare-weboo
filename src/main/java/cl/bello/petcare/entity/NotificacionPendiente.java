package cl.bello.petcare.entity;

import cl.bello.petcare.dto.NotificacionRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificaciones_pendientes", uniqueConstraints =
		@UniqueConstraint(name = "uk_notificacion_reserva_estado", columnNames = {"reserva_id", "estado_reserva"}))
public class NotificacionPendiente {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "clave_evento", nullable = false, unique = true, length = 36)
	private String claveEvento;
	@Column(name = "reserva_id", nullable = false)
	private Long reservaId;
	@Enumerated(EnumType.STRING)
	@Column(name = "estado_reserva", nullable = false, length = 20)
	private EstadoReserva estadoReserva;
	@Column(nullable = false, length = 254)
	private String destinatario;
	@Column(nullable = false, length = 200)
	private String asunto;
	@Column(nullable = false, length = 10000)
	private String mensaje;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EstadoNotificacion estado;
	@Column(nullable = false)
	private int intentos;
	@Column(name = "proximo_intento", nullable = false)
	private Instant proximoIntento;
	@Column(name = "bloqueada_hasta")
	private Instant bloqueadaHasta;
	@Column(name = "token_intento", length = 36)
	private String tokenIntento;
	@Column(name = "id_externo", length = 100)
	private String idExterno;
	@Column(name = "ultimo_error", length = 200)
	private String ultimoError;
	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;
	@Column(name = "actualizada_en", nullable = false)
	private Instant actualizadaEn;

	protected NotificacionPendiente() {
	}

	public NotificacionPendiente(Reserva reserva, Instant ahora) {
		claveEvento = UUID.randomUUID().toString();
		reservaId = reserva.getId();
		estadoReserva = reserva.getEstado();
		destinatario = reserva.getMascota().getCliente().getEmail();
		asunto = "Reserva " + estadoReserva.name().toLowerCase(java.util.Locale.ROOT);
		mensaje = "La reserva de " + reserva.getMascota().getNombre() + " para " + reserva.getFechaHora()
				+ " quedó " + estadoReserva.name().toLowerCase(java.util.Locale.ROOT) + ".";
		estado = EstadoNotificacion.PENDIENTE;
		creadaEn = ahora;
		actualizadaEn = ahora;
		proximoIntento = ahora;
	}

	public boolean disponible(Instant ahora) {
		return ((estado == EstadoNotificacion.PENDIENTE || estado == EstadoNotificacion.PENDIENTE_REINTENTO)
				&& !proximoIntento.isAfter(ahora))
				|| (estado == EstadoNotificacion.EN_PROCESO && !bloqueadaHasta.isAfter(ahora));
	}

	public void iniciarIntento(Instant ahora, Duration duracion) {
		estado = EstadoNotificacion.EN_PROCESO;
		intentos++;
		tokenIntento = UUID.randomUUID().toString();
		bloqueadaHasta = ahora.plus(duracion);
		actualizadaEn = ahora;
	}

	public void completar(EstadoNotificacion nuevoEstado, String externo, String error, Instant siguiente, Instant ahora) {
		estado = nuevoEstado;
		idExterno = externo;
		ultimoError = error;
		proximoIntento = siguiente;
		bloqueadaHasta = null;
		tokenIntento = null;
		actualizadaEn = ahora;
	}

	public NotificacionRequest solicitud() {
		return new NotificacionRequest(destinatario, "EMAIL", asunto, mensaje, reservaId);
	}

	public String resultado() {
		return estado.name() + ":" + (estado == EstadoNotificacion.ACEPTADA ? idExterno : claveEvento);
	}

	public Long getId() {
		return id;
	}
	public String getClaveEvento() {
		return claveEvento;
	}
	public Long getReservaId() {
		return reservaId;
	}
	public EstadoReserva getEstadoReserva() {
		return estadoReserva;
	}
	public EstadoNotificacion getEstado() {
		return estado;
	}
	public int getIntentos() {
		return intentos;
	}
	public Instant getProximoIntento() {
		return proximoIntento;
	}
	public Instant getBloqueadaHasta() {
		return bloqueadaHasta;
	}
	public String getTokenIntento() {
		return tokenIntento;
	}
	public String getUltimoError() {
		return ultimoError;
	}
}
