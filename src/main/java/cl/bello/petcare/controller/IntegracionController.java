package cl.bello.petcare.controller;

import cl.bello.petcare.dto.MedicamentoResponse;
import cl.bello.petcare.dto.UbicacionClinicaResponse;
import cl.bello.petcare.service.InformacionExternaClient;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integraciones")
public class IntegracionController {

	private final InformacionExternaClient informacionExternaClient;

	public IntegracionController(InformacionExternaClient informacionExternaClient) {
		this.informacionExternaClient = informacionExternaClient;
	}

	@GetMapping("/medicamentos")
	public List<MedicamentoResponse> buscarMedicamentos(@RequestParam String nombre) {
		return informacionExternaClient.buscarMedicamentos(nombre);
	}

	@GetMapping("/ubicacion-clinica")
	public ResponseEntity<UbicacionClinicaResponse> obtenerUbicacion() {
		return ResponseEntity.of(informacionExternaClient.obtenerUbicacion());
	}
}
