package cl.bello.petcare.service;

import cl.bello.petcare.dto.VeterinarioRequest;
import cl.bello.petcare.dto.VeterinarioResponse;
import cl.bello.petcare.entity.Veterinario;
import cl.bello.petcare.exception.ConflictoException;
import cl.bello.petcare.repository.VeterinarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeterinarioService {

	private final VeterinarioRepository veterinarioRepository;

	public VeterinarioService(VeterinarioRepository veterinarioRepository) {
		this.veterinarioRepository = veterinarioRepository;
	}

	@Transactional
	public VeterinarioResponse crear(VeterinarioRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (veterinarioRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictoException("Ya existe un veterinario con el email indicado");
		}
		Veterinario veterinario = new Veterinario(
				request.nombre().trim(),
				request.especialidad().trim(),
				email);
		return toResponse(veterinarioRepository.save(veterinario));
	}

	@Transactional(readOnly = true)
	public List<VeterinarioResponse> listar() {
		return veterinarioRepository.findByActivoTrueOrderByNombreAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	private VeterinarioResponse toResponse(Veterinario veterinario) {
		return new VeterinarioResponse(
				veterinario.getId(),
				veterinario.getNombre(),
				veterinario.getEspecialidad(),
				veterinario.getEmail(),
				veterinario.isActivo());
	}
}

