package cl.bello.petcare.controller;

import cl.bello.petcare.dto.ClienteRequest;
import cl.bello.petcare.dto.ClienteResponse;
import cl.bello.petcare.service.ClienteService;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/clientes")
public class ClienteController {

	private final ClienteService clienteService;

	public ClienteController(ClienteService clienteService) {
		this.clienteService = clienteService;
	}

	@PostMapping
	public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
		ClienteResponse response = clienteService.crear(request);
		return ResponseEntity.created(URI.create("/api/clientes/" + response.id())).body(response);
	}

	@GetMapping("/{id}")
	public ClienteResponse obtener(@PathVariable Long id) {
		return clienteService.obtener(id);
	}

	@PutMapping("/{id}")
	public ClienteResponse actualizar(
			@PathVariable Long id,
			@Valid @RequestBody ClienteRequest request) {
		return clienteService.actualizar(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		clienteService.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}

