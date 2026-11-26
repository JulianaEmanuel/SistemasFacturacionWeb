package com.facturacion.sistemaweb.security;

import java.util.Collection;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import java.sql.PreparedStatement;

/** Detecta de forma compatible la migración de versionado y revoca sesiones/tokenes. */
@Repository @Profile("postgres")
public class CredencialVersionRepository {
    private final JdbcTemplate jdbc;
    private volatile Boolean disponible;
    private volatile Boolean tablaRecuperacionDisponible;

    public CredencialVersionRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public boolean disponible() {
        Boolean cache=disponible;
        if(cache==null) synchronized(this) {
            cache=disponible;
            if(cache==null) disponible=cache=Integer.valueOf(1).equals(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='dbo' AND table_name='usuario' AND column_name='version_credencial'",Integer.class));
        }
        return cache;
    }

    public long version(long id) {
        if(!disponible()) return 0L;
        Long valor=jdbc.queryForObject("SELECT VERSION_CREDENCIAL FROM dbo.Usuario WHERE ID_USUARIO=?",Long.class,id);
        return valor==null?0L:valor;
    }

    public void bloquearCuenta(long id) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getAutoCommit()) {
                throw new InvalidDataAccessApiUsageException("El bloqueo de credenciales requiere una conexión transaccional.");
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, "SistemaFacturacionWeb.Credencial." + id);
                statement.execute();
            }
            return null;
        });
    }

    public boolean sesionVigente(long id,Collection<String> autoridades) {
        if(!disponible()) return true;
        String prefijo="CREDENTIAL_VERSION_";
        String version=autoridades.stream().filter(a->a.startsWith(prefijo)).map(a->a.substring(prefijo.length()))
            .findFirst().orElse(null);
        return version!=null && version.equals(Long.toString(version(id)));
    }

    public void incrementarYRevocar(long id) {
        if(disponible() && jdbc.update("UPDATE dbo.Usuario SET VERSION_CREDENCIAL=COALESCE(VERSION_CREDENCIAL,0)+1 WHERE ID_USUARIO=?",id)!=1)
            throw new DataAccessResourceFailureException("No se pudo incrementar la versión de credencial de la cuenta.");
        revocarRecuperaciones(id);
    }

    public void revocarRecuperaciones(long id) {
        if(tablaRecuperacionDisponible()) jdbc.update("UPDATE dbo.RecuperacionContrasenia SET REVOCADO_EN="
            + "(CURRENT_TIMESTAMP AT TIME ZONE 'UTC') "
            +"WHERE ID_USUARIO=? AND USADO_EN IS NULL AND REVOCADO_EN IS NULL",id);
    }

    private boolean tablaRecuperacionDisponible() {
        Boolean cache=tablaRecuperacionDisponible;
        if(cache==null) synchronized(this) {
            cache=tablaRecuperacionDisponible;
            if(cache==null) tablaRecuperacionDisponible=cache=Integer.valueOf(1).equals(jdbc.queryForObject(
                "SELECT CASE WHEN to_regclass('dbo.recuperacioncontrasenia') IS NULL THEN 0 ELSE 1 END",Integer.class));
        }
        return cache;
    }
}
