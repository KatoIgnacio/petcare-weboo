package cl.bello.petcare.repository;

import cl.bello.petcare.entity.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	List<Cliente> findByActivoTrueOrderByNombreAscIdAsc();

	Optional<Cliente> findByIdAndActivoTrue(Long id);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}

