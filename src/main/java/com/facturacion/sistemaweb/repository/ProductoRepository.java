package com.facturacion.sistemaweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.facturacion.sistemaweb.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	@Query("SELECT p FROM Producto p JOIN FETCH p.proveedor pr JOIN FETCH pr.persona "
			+ "ORDER BY p.nombreProducto")
	List<Producto> listarOrdenados();

	@Query("SELECT p FROM Producto p JOIN FETCH p.proveedor pr JOIN FETCH pr.persona "
			+ "WHERE LOWER(TRIM(p.nombreProducto)) LIKE LOWER(CONCAT('%', :filtro, '%')) "
			+ "ORDER BY p.nombreProducto")
	List<Producto> buscarPorNombre(@Param("filtro") String filtro);

	@Query("SELECT COUNT(d) > 0 FROM DetalleFactura d WHERE d.producto.idProducto = :idProducto")
	boolean tieneDetallesFactura(@Param("idProducto") Long idProducto);
}