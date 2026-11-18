package cl.bello.petcare.repository;

import cl.bello.petcare.dto.ReservaAgendaResponse;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Reserva;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	@Query("""
			select new cl.bello.petcare.dto.ReservaAgendaResponse(
				r.id, m.id, m.nombre, c.id, c.nombre,
				v.id, v.nombre, r.fechaHora, r.estado)
			from Reserva r
			join r.mascota m
			join m.cliente c
			join r.veterinario v
			where (cast(:desde as LocalDateTime) is null or r.fechaHora >= :desde)
			  and (cast(:hasta as LocalDateTime) is null or r.fechaHora < :hasta)
			  and (cast(:veterinarioId as Long) is null or v.id = :veterinarioId)
			order by r.fechaHora asc, r.id asc
			""")
	List<ReservaAgendaResponse> buscarAgenda(
			@Param("desde") LocalDateTime desde,
			@Param("hasta") LocalDateTime hasta,
			@Param("veterinarioId") Long veterinarioId);

	boolean existsByVeterinarioIdAndFechaHoraAndEstadoIn(
			Long veterinarioId,
			LocalDateTime fechaHora,
			Collection<EstadoReserva> estados);

	boolean existsByMascotaIdAndFechaHoraAndEstadoIn(
			Long mascotaId,
			LocalDateTime fechaHora,
			Collection<EstadoReserva> estados);

	boolean existsByVeterinarioIdAndFechaHoraAndEstadoInAndIdNot(
			Long veterinarioId,
			LocalDateTime fechaHora,
			Collection<EstadoReserva> estados,
			Long id);

	boolean existsByMascotaIdAndFechaHoraAndEstadoInAndIdNot(
			Long mascotaId,
			LocalDateTime fechaHora,
			Collection<EstadoReserva> estados,
			Long id);

	@EntityGraph(attributePaths = {"mascota", "mascota.cliente", "veterinario"})
	@Query("select r from Reserva r where r.id = :id")
	Optional<Reserva> buscarDetalle(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"mascota", "mascota.cliente", "veterinario"})
	@Query("select r from Reserva r where r.id = :id")
	Optional<Reserva> buscarParaActualizar(@Param("id") Long id);
}

