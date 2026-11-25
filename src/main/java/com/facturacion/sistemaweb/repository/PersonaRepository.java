package com.facturacion.sistemaweb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.facturacion.sistemaweb.model.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

	@Query("SELECT p FROM Persona p "
			+ "WHERE LOWER(TRIM(p.cedulaPersona)) LIKE LOWER(CONCAT('%', :filtro, '%')) "
			+ "OR LOWER(TRIM(p.nombresPersona)) LIKE LOWER(CONCAT('%', :filtro, '%')) "
			+ "OR LOWER(TRIM(p.apellidosPersona)) LIKE LOWER(CONCAT('%', :filtro, '%')) "
			+ "ORDER BY p.apellidosPersona, p.nombresPersona")
	List<Persona> buscar(@Param("filtro") String filtro);

	@Query("SELECT p FROM Persona p ORDER BY p.apellidosPersona, p.nombresPersona")
	List<Persona> listarOrdenadas();

	@Query("SELECT COUNT(p) > 0 FROM Persona p WHERE TRIM(p.cedulaPersona) = :cedula")
	boolean existeCedula(@Param("cedula") String cedula);

	@Query("SELECT COUNT(p) > 0 FROM Persona p WHERE TRIM(p.cedulaPersona) = :cedula AND p.idPersona <> :idPersona")
	boolean existeCedulaEnOtraPersona(@Param("cedula") String cedula, @Param("idPersona") Long idPersona);

	@Query("SELECT COUNT(f) > 0 FROM Factura f WHERE f.idPersona = :idPersona")
	boolean tieneFacturas(@Param("idPersona") Long idPersona);

	@Query("SELECT COUNT(e) > 0 FROM Empleado e WHERE e.persona.idPersona = :idPersona")
	boolean esEmpleado(@Param("idPersona") Long idPersona);

	@Query("SELECT COUNT(pr) > 0 FROM Proveedor pr WHERE pr.persona.idPersona = :idPersona")
	boolean esProveedor(@Param("idPersona") Long idPersona);
}