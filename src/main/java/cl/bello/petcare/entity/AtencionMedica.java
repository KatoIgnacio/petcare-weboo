package cl.bello.petcare.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "atenciones_medicas")
public class AtencionMedica {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reserva_id", nullable = false, unique = true)
	private Reserva reserva;

	@Column(nullable = false)
	private LocalDateTime fecha = LocalDateTime.now();

	@Column(nullable = false, length = 1000)
	private String diagnostico;

	@Column(nullable = false, length = 1000)
	private String tratamiento;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "atencion_medicamentos", joinColumns = @JoinColumn(name = "atencion_id"))
	@Column(name = "medicamento", nullable = false, length = 120)
	private List<String> medicamentos = new ArrayList<>();

	@Column(length = 1500)
	private String observaciones;

	protected AtencionMedica() {
	}

	public AtencionMedica(
			Reserva reserva,
			String diagnostico,
			String tratamiento,
			List<String> medicamentos,
			String observaciones) {
		this.reserva = reserva;
		this.diagnostico = diagnostico;
		this.tratamiento = tratamiento;
		this.medicamentos = new ArrayList<>(medicamentos);
		this.observaciones = observaciones;
	}

	public Long getId() {
		return id;
	}

	public Reserva getReserva() {
		return reserva;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public String getDiagnostico() {
		return diagnostico;
	}

	public String getTratamiento() {
		return tratamiento;
	}

	public List<String> getMedicamentos() {
		return List.copyOf(medicamentos);
	}

	public String getObservaciones() {
		return observaciones;
	}
}

