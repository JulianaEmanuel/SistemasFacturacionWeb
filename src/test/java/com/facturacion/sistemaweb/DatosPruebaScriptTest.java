package com.facturacion.sistemaweb;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DatosPruebaScriptTest {
    @Test void datosFicticiosGuardanElAmbienteYEvitanDuplicados() throws Exception {
        String sql=Files.readString(Path.of("db/datos_prueba_postgresql.sql"));
        assertTrue(sql.contains("current_database() <> 'sistemafacturacionweb_pruebas'"));
        assertTrue(sql.contains("pg_advisory_xact_lock"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
        assertTrue(sql.contains("example.invalid"));
        assertFalse(sql.contains("INSERT INTO dbo.Factura"));
    }

    @Test void esquemaCreaLasTablasDeFacturacionYSeguridad() throws Exception {
        String sql=Files.readString(Path.of("db/esquema_postgresql.sql"));
        for (String tabla : new String[]{"FacturaPago","FacturaCliente","VerificacionCorreo","RecuperacionContrasenia","FacturaSri","EmisorSri"}) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS dbo."+tabla+" ("),tabla);
        }
    }
}
