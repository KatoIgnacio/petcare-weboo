package cl.bello.petcare.repository;

import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.entity.Reserva;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

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

