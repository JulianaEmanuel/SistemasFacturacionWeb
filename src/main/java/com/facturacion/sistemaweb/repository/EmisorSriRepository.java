package com.facturacion.sistemaweb.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.facturacion.sistemaweb.model.EmisorSri;

public interface EmisorSriRepository extends JpaRepository<EmisorSri, Integer> {
    Optional<EmisorSri> findFirstByActivoTrueOrderByIdEmisorAsc();
}
