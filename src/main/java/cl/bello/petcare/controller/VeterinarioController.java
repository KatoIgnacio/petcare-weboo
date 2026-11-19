package cl.bello.petcare.controller;

import cl.bello.petcare.dto.VeterinarioRequest;
import cl.bello.petcare.dto.VeterinarioResponse;
import cl.bello.petcare.service.ReservaService;
import cl.bello.petcare.service.VeterinarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinarios")
public class VeterinarioController {

	private final VeterinarioService veterinarioService;
	private final ReservaService reservaService;

	public VeterinarioController(VeterinarioService veterinarioService, ReservaService reservaService) {
		this.veterinarioService = veterinarioService;
		this.reservaService = reservaService;
	}

	@PostMapping
	public ResponseEntity<VeterinarioResponse> crear(@Valid @RequestBody VeterinarioRequest request) {
		VeterinarioResponse response = veterinarioService.crear(request);
		return ResponseEntity.created(URI.create("/api/veterinarios/" + response.id())).body(response);
	}

	@GetMapping
	public List<VeterinarioResponse> listar() {
		return veterinarioService.listar();
	}

	@GetMapping("/{id}/disponibilidad")
	public List<String> disponibilidad(@PathVariable Long id, @RequestParam String fecha,
			@RequestParam(required = false) Long reservaId) {
		if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new IllegalArgumentException("fecha debe tener formato YYYY-MM-DD");
		}
		try {
			return reservaService.disponibilidad(id, LocalDate.parse(fecha), reservaId);
		} catch (DateTimeParseException error) {
			throw new IllegalArgumentException("fecha debe ser una fecha válida en formato YYYY-MM-DD");
		}
	}
}

