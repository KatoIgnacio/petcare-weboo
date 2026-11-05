package cl.bello.petcare.repository;

import cl.bello.petcare.entity.AtencionMedica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {

	Optional<AtencionMedica> findByReservaId(Long reservaId);

	List<AtencionMedica> findByReservaMascotaIdOrderByFechaDesc(Long mascotaId);
}

