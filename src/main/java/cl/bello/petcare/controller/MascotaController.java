package cl.bello.petcare.controller;

import cl.bello.petcare.dto.MascotaRequest;
import cl.bello.petcare.dto.MascotaResponse;
import cl.bello.petcare.service.MascotaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MascotaController {

	private final MascotaService mascotaService;

	public MascotaController(MascotaService mascotaService) {
		this.mascotaService = mascotaService;
	}

	@PostMapping("/clientes/{clienteId}/mascotas")
	public ResponseEntity<MascotaResponse> crear(
			@PathVariable Long clienteId,
			@Valid @RequestBody MascotaRequest request) {
		MascotaResponse response = mascotaService.crear(clienteId, request);
		return ResponseEntity.created(URI.create("/api/mascotas/" + response.id())).body(response);
	}

	@GetMapping("/clientes/{clienteId}/mascotas")
	public List<MascotaResponse> listarPorCliente(@PathVariable Long clienteId) {
		return mascotaService.listarPorCliente(clienteId);
	}

	@GetMapping("/mascotas/{id}")
	public MascotaResponse obtener(@PathVariable Long id) {
		return mascotaService.obtener(id);
	}

	@PutMapping("/mascotas/{id}")
	public MascotaResponse actualizar(
			@PathVariable Long id,
			@Valid @RequestBody MascotaRequest request) {
		return mascotaService.actualizar(id, request);
	}

	@DeleteMapping("/mascotas/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		mascotaService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}

