package cl.bello.petcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
public class Reserva {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "mascota_id", nullable = false)
	private Mascota mascota;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "veterinario_id", nullable = false)
	private Veterinario veterinario;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoReserva estado = EstadoReserva.PENDIENTE;

	@Column(name = "creada_en", nullable = false)
	private LocalDateTime creadaEn = LocalDateTime.now();

	@Version
	@Column(nullable = false)
	private long version;

	protected Reserva() {
	}

	public Reserva(Mascota mascota, Veterinario veterinario, LocalDateTime fechaHora) {
		this.mascota = mascota;
		this.veterinario = veterinario;
		this.fechaHora = fechaHora;
	}

	public Long getId() {
		return id;
	}

	public Mascota getMascota() {
		return mascota;
	}

	public Veterinario getVeterinario() {
		return veterinario;
	}

	public void setVeterinario(Veterinario veterinario) {
		this.veterinario = veterinario;
	}

	public LocalDateTime getFechaHora() {
		return fechaHora;
	}

	public void setFechaHora(LocalDateTime fechaHora) {
		this.fechaHora = fechaHora;
	}

	public EstadoReserva getEstado() {
		return estado;
	}

	public void setEstado(EstadoReserva estado) {
		this.estado = estado;
	}

	public LocalDateTime getCreadaEn() {
		return creadaEn;
	}
}

