BEGIN;

DO $$
DECLARE
    proveedor_demo bigint;
BEGIN
    IF current_database() <> 'sistemafacturacionweb_pruebas' THEN
        RAISE EXCEPTION 'Este script requiere la base sistemafacturacionweb_pruebas';
    END IF;

    PERFORM pg_advisory_xact_lock(hashtext('facturacion.datos_prueba'));

    INSERT INTO dbo.Persona (cedula_persona, nombres_persona, apellidos_persona, correo_persona)
    SELECT datos.identificacion, datos.nombres, datos.apellidos, datos.correo
    FROM (VALUES
        ('0000000000', 'Cliente', 'Demostración', 'cliente@example.invalid'),
        ('0000000001', 'Proveedor', 'Demostración', 'proveedor@example.invalid')
    ) AS datos(identificacion, nombres, apellidos, correo)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Persona p WHERE p.cedula_persona = datos.identificacion
    );

    INSERT INTO dbo.Proveedor (id_persona_proveedor, email)
    SELECT p.id_persona, p.correo_persona
    FROM dbo.Persona p
    WHERE p.cedula_persona = '0000000001'
      AND NOT EXISTS (SELECT 1 FROM dbo.Proveedor v WHERE v.id_persona_proveedor = p.id_persona);

    SELECT v.id_proveedor INTO STRICT proveedor_demo
    FROM dbo.Proveedor v JOIN dbo.Persona p ON p.id_persona = v.id_persona_proveedor
    WHERE p.cedula_persona = '0000000001';

    INSERT INTO dbo.Producto (id_proveedor, nombre_producto, precio_unitario, iva_producto)
    SELECT proveedor_demo, datos.nombre, datos.precio, datos.iva
    FROM (VALUES ('Cuaderno demo', 2.50, 15.00), ('Libro demo', 8.00, 0.00)) AS datos(nombre, precio, iva)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Producto p WHERE p.id_proveedor = proveedor_demo AND p.nombre_producto = datos.nombre
    );
END
$$;

COMMIT;
