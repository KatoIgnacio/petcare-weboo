package cl.bello.petcare.repository;

import cl.bello.petcare.entity.AtencionMedica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AtencionMedicaRepository extends JpaRepository<AtencionMedica, Long> {

	Optional<AtencionMedica> findByReservaId(Long reservaId);

	@EntityGraph(attributePaths = {"reserva", "medicamentos"})
	List<AtencionMedica> findByReservaMascotaIdOrderByFechaDescIdDesc(Long mascotaId);
}

