package cl.bello.petcare.controller;

import cl.bello.petcare.dto.AtencionRequest;
import cl.bello.petcare.dto.AtencionResponse;
import cl.bello.petcare.dto.EstadoReservaRequest;
import cl.bello.petcare.dto.ReservaAgendaResponse;
import cl.bello.petcare.dto.ReservaRequest;
import cl.bello.petcare.dto.ReservaResponse;
import cl.bello.petcare.dto.ReservaUpdateRequest;
import cl.bello.petcare.service.ReservaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

	private final ReservaService reservaService;

	public ReservaController(ReservaService reservaService) {
		this.reservaService = reservaService;
	}

	@PostMapping
	public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
		ReservaResponse response = reservaService.crear(request);
		return ResponseEntity.created(URI.create("/api/reservas/" + response.id())).body(response);
	}

	@GetMapping("/{id}")
	public ReservaResponse obtener(@PathVariable Long id) {
		return reservaService.obtener(id);
	}

	@GetMapping
	public List<ReservaAgendaResponse> listar(
			@RequestParam(required = false) String fecha,
			@RequestParam(required = false) String veterinarioId) {
		if (fecha != null && !fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
			throw new IllegalArgumentException("fecha debe tener formato YYYY-MM-DD");
		}
		LocalDate dia;
		Long profesionalId;
		try {
			dia = fecha == null ? null : LocalDate.parse(fecha);
			profesionalId = veterinarioId == null ? null : Long.valueOf(veterinarioId);
		} catch (DateTimeParseException | NumberFormatException error) {
			throw new IllegalArgumentException(
					"Los filtros requieren una fecha válida YYYY-MM-DD y veterinarioId entero positivo");
		}
		return reservaService.listar(dia, profesionalId);
	}

	@PutMapping("/{id}")
	public ReservaResponse actualizar(
			@PathVariable Long id,
			@Valid @RequestBody ReservaUpdateRequest request) {
		return reservaService.actualizar(id, request);
	}

	@PatchMapping("/{id}/estado")
	public ReservaResponse cambiarEstado(
			@PathVariable Long id,
			@Valid @RequestBody EstadoReservaRequest request) {
		return reservaService.cambiarEstado(id, request);
	}

	@PostMapping("/{id}/atencion")
	public ResponseEntity<AtencionResponse> registrarAtencion(
			@PathVariable Long id,
			@Valid @RequestBody AtencionRequest request) {
		AtencionResponse response = reservaService.registrarAtencion(id, request);
		return ResponseEntity.created(URI.create("/api/reservas/" + id + "/atencion")).body(response);
	}
}

