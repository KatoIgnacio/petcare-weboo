package cl.bello.petcare.service;

import cl.bello.petcare.dto.MascotaRequest;
import cl.bello.petcare.dto.MascotaResponse;
import cl.bello.petcare.entity.Cliente;
import cl.bello.petcare.entity.Mascota;
import cl.bello.petcare.exception.RecursoNoEncontradoException;
import cl.bello.petcare.repository.ClienteRepository;
import cl.bello.petcare.repository.MascotaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MascotaService {

	private final ClienteRepository clienteRepository;
	private final MascotaRepository mascotaRepository;

	public MascotaService(ClienteRepository clienteRepository, MascotaRepository mascotaRepository) {
		this.clienteRepository = clienteRepository;
		this.mascotaRepository = mascotaRepository;
	}

	@Transactional
	public MascotaResponse crear(Long clienteId, MascotaRequest request) {
		Cliente cliente = clienteRepository.findByIdAndActivoTrue(clienteId)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
		Mascota mascota = new Mascota(
				cliente,
				request.nombre().trim(),
				request.especie().trim(),
				request.raza().trim(),
				request.fechaNacimiento(),
				request.peso());
		return toResponse(mascotaRepository.save(mascota));
	}

	@Transactional(readOnly = true)
	public List<MascotaResponse> listarPorCliente(Long clienteId) {
		if (clienteRepository.findByIdAndActivoTrue(clienteId).isEmpty()) {
			throw new RecursoNoEncontradoException("Cliente no encontrado");
		}
		return mascotaRepository.findByClienteIdAndActivoTrueOrderByNombreAsc(clienteId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public MascotaResponse obtener(Long id) {
		return toResponse(buscarActiva(id));
	}

	@Transactional
	public MascotaResponse actualizar(Long id, MascotaRequest request) {
		Mascota mascota = buscarActiva(id);
		mascota.setNombre(request.nombre().trim());
		mascota.setEspecie(request.especie().trim());
		mascota.setRaza(request.raza().trim());
		mascota.setFechaNacimiento(request.fechaNacimiento());
		mascota.setPeso(request.peso());
		return toResponse(mascota);
	}

	@Transactional
	public void eliminar(Long id) {
		buscarActiva(id).setActivo(false);
	}

	private Mascota buscarActiva(Long id) {
		return mascotaRepository.findByIdAndActivoTrue(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
	}

	private MascotaResponse toResponse(Mascota mascota) {
		return new MascotaResponse(
				mascota.getId(),
				mascota.getCliente().getId(),
				mascota.getNombre(),
				mascota.getEspecie(),
				mascota.getRaza(),
				mascota.getFechaNacimiento(),
				mascota.getPeso(),
				mascota.isActivo());
	}
}

