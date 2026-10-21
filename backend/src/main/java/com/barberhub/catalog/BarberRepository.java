package com.barberhub.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Sin filtros por tenant a mano: Hibernate agrega "tenant_id = ?" gracias a @TenantId.
public interface BarberRepository extends JpaRepository<Barber, Long> {

    List<Barber> findAllByOrderByNameAsc();

    List<Barber> findByActiveTrueOrderByNameAsc();

    /** Bloquea la fila del barbero para serializar reservas simultáneas sobre él. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barber b where b.id = :id")
    Optional<Barber> findByIdForUpdate(@Param("id") Long id);
}
