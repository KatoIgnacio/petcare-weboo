package cl.bello.petcare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veterinarios")
public class Veterinario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 120)
	private String especialidad;

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(nullable = false)
	private boolean activo = true;

	protected Veterinario() {
	}

	public Veterinario(String nombre, String especialidad, String email) {
		this.nombre = nombre;
		this.especialidad = especialidad;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEspecialidad() {
		return especialidad;
	}

	public String getEmail() {
		return email;
	}

	public boolean isActivo() {
		return activo;
	}
}

