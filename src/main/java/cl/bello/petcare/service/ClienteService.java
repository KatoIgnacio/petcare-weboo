package cl.bello.petcare.service;

import cl.bello.petcare.dto.ClienteRequest;
import cl.bello.petcare.dto.ClienteResponse;
import cl.bello.petcare.entity.Cliente;
import cl.bello.petcare.exception.ConflictoException;
import cl.bello.petcare.exception.RecursoNoEncontradoException;
import cl.bello.petcare.repository.ClienteRepository;
import cl.bello.petcare.repository.MascotaRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

	private final ClienteRepository clienteRepository;
	private final MascotaRepository mascotaRepository;

	public ClienteService(ClienteRepository clienteRepository, MascotaRepository mascotaRepository) {
		this.clienteRepository = clienteRepository;
		this.mascotaRepository = mascotaRepository;
	}

	@Transactional
	public ClienteResponse crear(ClienteRequest request) {
		String email = normalizarEmail(request.email());
		if (clienteRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictoException("Ya existe un cliente con el email indicado");
		}
		Cliente cliente = new Cliente(
				request.nombre().trim(),
				email,
				request.telefono().trim());
		return toResponse(clienteRepository.save(cliente));
	}

	@Transactional(readOnly = true)
	public ClienteResponse obtener(Long id) {
		return toResponse(buscarActivo(id));
	}

	@Transactional
	public ClienteResponse actualizar(Long id, ClienteRequest request) {
		Cliente cliente = buscarActivo(id);
		String email = normalizarEmail(request.email());
		if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw new ConflictoException("Ya existe otro cliente con el email indicado");
		}
		cliente.setNombre(request.nombre().trim());
		cliente.setEmail(email);
		cliente.setTelefono(request.telefono().trim());
		return toResponse(cliente);
	}

	@Transactional
	public void eliminar(Long id) {
		Cliente cliente = buscarActivo(id);
		cliente.setActivo(false);
		mascotaRepository.findByClienteIdAndActivoTrueOrderByNombreAsc(id)
				.forEach(mascota -> mascota.setActivo(false));
	}

	private Cliente buscarActivo(Long id) {
		return clienteRepository.findByIdAndActivoTrue(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
	}

	private String normalizarEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private ClienteResponse toResponse(Cliente cliente) {
		return new ClienteResponse(
				cliente.getId(),
				cliente.getNombre(),
				cliente.getEmail(),
				cliente.getTelefono(),
				cliente.isActivo());
	}
}

