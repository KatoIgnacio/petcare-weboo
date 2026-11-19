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

	// Otra cita de 30 minutos se solapa cuando su inicio queda estrictamente entre ambos límites.
	@Query("""
			select r.fechaHora from Reserva r
			where r.veterinario.id = :veterinarioId and r.estado in :estados
			  and r.fechaHora > :desde and r.fechaHora < :hasta
			  and (cast(:excluida as Long) is null or r.id <> :excluida)
			""")
	List<LocalDateTime> horariosOcupados(
			Long veterinarioId,
			LocalDateTime desde,
			LocalDateTime hasta,
			Collection<EstadoReserva> estados,
			Long excluida);

	@Query("""
			select count(r) > 0 from Reserva r
			where r.mascota.id = :mascotaId and r.estado in :estados
			  and r.fechaHora > :desde and r.fechaHora < :hasta
			  and (cast(:excluida as Long) is null or r.id <> :excluida)
			""")
	boolean mascotaOcupada(
			Long mascotaId,
			LocalDateTime desde,
			LocalDateTime hasta,
			Collection<EstadoReserva> estados,
			Long excluida);

	@EntityGraph(attributePaths = {"mascota", "mascota.cliente", "veterinario"})
	@Query("select r from Reserva r where r.id = :id")
	Optional<Reserva> buscarDetalle(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"mascota", "mascota.cliente", "veterinario"})
	@Query("select r from Reserva r where r.id = :id")
	Optional<Reserva> buscarParaActualizar(@Param("id") Long id);
}

