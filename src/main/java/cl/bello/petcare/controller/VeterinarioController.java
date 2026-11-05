package cl.bello.petcare.controller;

import cl.bello.petcare.dto.VeterinarioRequest;
import cl.bello.petcare.dto.VeterinarioResponse;
import cl.bello.petcare.service.VeterinarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veterinarios")
public class VeterinarioController {

	private final VeterinarioService veterinarioService;

	public VeterinarioController(VeterinarioService veterinarioService) {
		this.veterinarioService = veterinarioService;
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
}

