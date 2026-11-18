package cl.bello.petcare.repository;

import cl.bello.petcare.entity.EstadoNotificacion;
import cl.bello.petcare.entity.NotificacionPendiente;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<NotificacionPendiente, Long> {
	Optional<NotificacionPendiente> findFirstByReservaIdOrderByIdDesc(Long reservaId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select n from NotificacionPendiente n where n.id = :id")
	Optional<NotificacionPendiente> buscarParaActualizar(@Param("id") Long id);

	@Query("""
			select n.id from NotificacionPendiente n
			where (n.estado in :pendientes and n.proximoIntento <= :ahora)
			   or (n.estado = :procesando and n.bloqueadaHasta <= :ahora)
			order by n.id asc
			""")
	List<Long> buscarDisponibles(@Param("pendientes") Collection<EstadoNotificacion> pendientes,
			@Param("procesando") EstadoNotificacion procesando, @Param("ahora") Instant ahora, Pageable pagina);
}
