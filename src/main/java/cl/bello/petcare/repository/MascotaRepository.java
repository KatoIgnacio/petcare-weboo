package cl.bello.petcare.repository;

import cl.bello.petcare.entity.Mascota;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

	Optional<Mascota> findByIdAndActivoTrue(Long id);

	List<Mascota> findByClienteIdAndActivoTrueOrderByNombreAsc(Long clienteId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select m from Mascota m
			join fetch m.cliente
			where m.id = :id and m.activo = true
			""")
	Optional<Mascota> buscarActivaParaActualizar(@Param("id") Long id);
}

