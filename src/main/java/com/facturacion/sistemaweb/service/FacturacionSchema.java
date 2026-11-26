package com.facturacion.sistemaweb.service;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Detecta migraciones manuales; nunca crea tablas ni completa registros. */
@Service @Profile("postgres")
public class FacturacionSchema {
    private final JdbcTemplate jdbc;
    private volatile Boolean contactos,disponible;
    public FacturacionSchema(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public boolean contactos(){
        if(contactos==null) contactos=jdbc.queryForObject("SELECT CASE WHEN COUNT(*)=3 THEN 1 ELSE 0 END FROM information_schema.columns WHERE table_schema='dbo' AND table_name='persona' AND column_name IN ('direccion_persona','telefono_persona','correo_persona')",Integer.class)==1;
        return contactos;
    }
    public boolean disponible(){
        if(disponible==null) disponible=contactos() && jdbc.queryForObject("SELECT CASE WHEN to_regclass('dbo.formapago') IS NOT NULL AND to_regclass('dbo.facturacliente') IS NOT NULL AND COUNT(*)=2 THEN 1 ELSE 0 END FROM information_schema.columns WHERE table_schema='dbo' AND table_name='facturapago' AND column_name IN ('referencia','cuotas')",Integer.class)==1;
        return disponible;
    }
    public void exigirDisponible(){if(!disponible())throw new FacturaException("Faltan las tablas o columnas de contactos y pagos. No se guardó la factura. Solicita su aplicación autorizada y reinicia la aplicación.");}
}
