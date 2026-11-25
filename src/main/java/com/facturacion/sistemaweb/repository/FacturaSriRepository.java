package com.facturacion.sistemaweb.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.facturacion.sistemaweb.model.FacturaSri;

public interface FacturaSriRepository extends JpaRepository<FacturaSri, Long> {
    Optional<FacturaSri> findByIdFactura(Long idFactura);
}
