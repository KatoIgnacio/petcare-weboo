package cl.bello.petcare.repository;

import cl.bello.petcare.entity.Veterinario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {

	Optional<Veterinario> findByIdAndActivoTrue(Long id);

	List<Veterinario> findByActivoTrueOrderByNombreAsc();

	boolean existsByEmailIgnoreCase(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Veterinario v where v.id = :id and v.activo = true")
	Optional<Veterinario> buscarActivoParaActualizar(@Param("id") Long id);
}

