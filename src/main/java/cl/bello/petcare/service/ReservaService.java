package cl.bello.petcare.service;

import cl.bello.petcare.dto.AtencionRequest;
import cl.bello.petcare.dto.AtencionResponse;
import cl.bello.petcare.dto.EstadoReservaRequest;
import cl.bello.petcare.dto.ReservaRequest;
import cl.bello.petcare.dto.ReservaResponse;
import cl.bello.petcare.dto.ReservaUpdateRequest;
import cl.bello.petcare.entity.AtencionMedica;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Mascota;
import cl.bello.petcare.entity.Reserva;
import cl.bello.petcare.entity.Veterinario;
import cl.bello.petcare.exception.ConflictoException;
import cl.bello.petcare.exception.RecursoNoEncontradoException;
import cl.bello.petcare.repository.AtencionMedicaRepository;
import cl.bello.petcare.repository.MascotaRepository;
import cl.bello.petcare.repository.ReservaRepository;
import cl.bello.petcare.repository.VeterinarioRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

	private static final Set<EstadoReserva> ESTADOS_ACTIVOS =
			EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

	private final MascotaRepository mascotaRepository;
	private final VeterinarioRepository veterinarioRepository;
	private final ReservaRepository reservaRepository;
	private final AtencionMedicaRepository atencionRepository;
	private final NotificacionClient notificacionClient;

	public ReservaService(
			MascotaRepository mascotaRepository,
			VeterinarioRepository veterinarioRepository,
			ReservaRepository reservaRepository,
			AtencionMedicaRepository atencionRepository,
			NotificacionClient notificacionClient) {
		this.mascotaRepository = mascotaRepository;
		this.veterinarioRepository = veterinarioRepository;
		this.reservaRepository = reservaRepository;
		this.atencionRepository = atencionRepository;
		this.notificacionClient = notificacionClient;
	}

	@Transactional
	public ReservaResponse crear(ReservaRequest request) {
		validarFechaFutura(request.fechaHora());
		Mascota mascota = mascotaRepository.buscarActivaParaActualizar(request.mascotaId())
				.orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
		Veterinario veterinario = veterinarioRepository.buscarActivoParaActualizar(request.veterinarioId())
				.orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
		validarDisponibilidad(mascota.getId(), veterinario.getId(), request.fechaHora(), null);

		Reserva reserva = reservaRepository.save(new Reserva(mascota, veterinario, request.fechaHora()));
		return toResponse(reserva, "NO_APLICA", null);
	}

	@Transactional(readOnly = true)
	public ReservaResponse obtener(Long id) {
		Reserva reserva = buscarDetalle(id);
		AtencionMedica atencion = atencionRepository.findByReservaId(id).orElse(null);
		return toResponse(reserva, "NO_APLICA", atencion);
	}

	@Transactional
	public ReservaResponse actualizar(Long id, ReservaUpdateRequest request) {
		validarFechaFutura(request.fechaHora());
		Reserva reserva = buscarParaActualizar(id);
		if (reserva.getEstado() == EstadoReserva.CANCELADA
				|| reserva.getEstado() == EstadoReserva.ATENDIDA) {
			throw new ConflictoException("La reserva ya no puede ser modificada");
		}
		Mascota mascota = mascotaRepository.buscarActivaParaActualizar(reserva.getMascota().getId())
				.orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada"));
		Veterinario veterinario = veterinarioRepository.buscarActivoParaActualizar(request.veterinarioId())
				.orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
		validarDisponibilidad(mascota.getId(), veterinario.getId(), request.fechaHora(), id);

		reserva.setVeterinario(veterinario);
		reserva.setFechaHora(request.fechaHora());
		return toResponse(reserva, "NO_APLICA", atencionRepository.findByReservaId(id).orElse(null));
	}

	@Transactional
	public ReservaResponse cambiarEstado(Long id, EstadoReservaRequest request) {
		Reserva reserva = buscarParaActualizar(id);
		if (reserva.getEstado() == request.estado()) {
			return toResponse(
					reserva,
					"NO_APLICA",
					atencionRepository.findByReservaId(id).orElse(null));
		}
		if (!transicionPermitida(reserva.getEstado(), request.estado())) {
			throw new ConflictoException("La transición de estado no está permitida");
		}
		if (request.estado() == EstadoReserva.ATENDIDA
				&& atencionRepository.findByReservaId(id).isEmpty()) {
			throw new ConflictoException("Debe registrar la atención médica antes de finalizar la reserva");
		}

		reserva.setEstado(request.estado());
		reservaRepository.saveAndFlush(reserva);
		String notificacion = notificacionClient.enviarCambioReserva(reserva);
		return toResponse(reserva, notificacion, atencionRepository.findByReservaId(id).orElse(null));
	}

	@Transactional
	public AtencionResponse registrarAtencion(Long reservaId, AtencionRequest request) {
		Reserva reserva = buscarParaActualizar(reservaId);
		if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
			throw new ConflictoException("Solo una reserva confirmada puede registrar atención");
		}
		if (atencionRepository.findByReservaId(reservaId).isPresent()) {
			throw new ConflictoException("La reserva ya posee una atención médica");
		}

		AtencionMedica atencion = new AtencionMedica(
				reserva,
				request.diagnostico().trim(),
				request.tratamiento().trim(),
				request.medicamentos().stream().map(String::trim).toList(),
				request.observaciones() == null ? null : request.observaciones().trim());
		AtencionMedica guardada = atencionRepository.save(atencion);
		reserva.setEstado(EstadoReserva.ATENDIDA);
		return toAtencionResponse(guardada);
	}

	private void validarDisponibilidad(
			Long mascotaId,
			Long veterinarioId,
			LocalDateTime fechaHora,
			Long reservaExcluida) {
		boolean veterinarioOcupado;
		boolean mascotaOcupada;
		if (reservaExcluida == null) {
			veterinarioOcupado = reservaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoIn(
					veterinarioId, fechaHora, ESTADOS_ACTIVOS);
			mascotaOcupada = reservaRepository.existsByMascotaIdAndFechaHoraAndEstadoIn(
					mascotaId, fechaHora, ESTADOS_ACTIVOS);
		} else {
			veterinarioOcupado = reservaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoInAndIdNot(
					veterinarioId, fechaHora, ESTADOS_ACTIVOS, reservaExcluida);
			mascotaOcupada = reservaRepository.existsByMascotaIdAndFechaHoraAndEstadoInAndIdNot(
					mascotaId, fechaHora, ESTADOS_ACTIVOS, reservaExcluida);
		}
		if (veterinarioOcupado) {
			throw new ConflictoException("El veterinario ya posee una reserva activa en ese horario");
		}
		if (mascotaOcupada) {
			throw new ConflictoException("La mascota ya posee una reserva activa en ese horario");
		}
	}

	private boolean transicionPermitida(EstadoReserva actual, EstadoReserva siguiente) {
		return switch (actual) {
			case PENDIENTE -> siguiente == EstadoReserva.CONFIRMADA
					|| siguiente == EstadoReserva.CANCELADA;
			case CONFIRMADA -> siguiente == EstadoReserva.CANCELADA;
			case CANCELADA, ATENDIDA -> false;
		};
	}

	private void validarFechaFutura(LocalDateTime fechaHora) {
		if (!fechaHora.isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("La fecha y hora deben estar en el futuro");
		}
	}

	private Reserva buscarDetalle(Long id) {
		return reservaRepository.buscarDetalle(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
	}

	private Reserva buscarParaActualizar(Long id) {
		return reservaRepository.buscarParaActualizar(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
	}

	private ReservaResponse toResponse(
			Reserva reserva,
			String resultadoNotificacion,
			AtencionMedica atencion) {
		return new ReservaResponse(
				reserva.getId(),
				reserva.getMascota().getId(),
				reserva.getMascota().getNombre(),
				reserva.getMascota().getCliente().getId(),
				reserva.getVeterinario().getId(),
				reserva.getVeterinario().getNombre(),
				reserva.getFechaHora(),
				reserva.getEstado(),
				resultadoNotificacion,
				atencion == null ? null : toAtencionResponse(atencion));
	}

	private AtencionResponse toAtencionResponse(AtencionMedica atencion) {
		return new AtencionResponse(
				atencion.getId(),
				atencion.getReserva().getId(),
				atencion.getFecha(),
				atencion.getDiagnostico(),
				atencion.getTratamiento(),
				atencion.getMedicamentos(),
				atencion.getObservaciones());
	}
}

