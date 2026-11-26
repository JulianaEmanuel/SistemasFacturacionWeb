package com.facturacion.sistemaweb.repository;

import java.sql.*;
import java.util.Arrays;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Aplica ventanas móviles por destino e IP sin conservar correo ni IP en claro. */
@Repository @Profile("postgres")
public class IntentoRecuperacionRepository {
    private final JdbcTemplate jdbc;
    public IntentoRecuperacionRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional
    public boolean permitir(byte tipo,byte[] huellaDestino,byte[] huellaIp) {
        byte[] ip=huellaIp.clone(),destino=huellaDestino.clone();
        try {
            bloquear(tipo,destino);
            bloquear((byte)0,ip);
            String ahora = "(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')";
            jdbc.update("DELETE FROM dbo.IntentoRecuperacion WHERE ID_INTENTO IN (SELECT ID_INTENTO FROM dbo.IntentoRecuperacion WHERE CREADO_EN<"+ahora+" - INTERVAL '1 day' LIMIT 1000)");
            String horaAnterior = ahora+" - INTERVAL '1 hour'";
            String quinceMinutos = ahora+" - INTERVAL '15 minutes'";
            Long destinoPrevio=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.IntentoRecuperacion "
                +"WHERE TIPO=? AND HUELLA=? AND CREADO_EN>="+horaAnterior,Long.class,tipo,destino);
            Long ipPrevio=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.IntentoRecuperacion "
                +"WHERE TIPO=0 AND HUELLA=? AND CREADO_EN>="+quinceMinutos,Long.class,ip);
            jdbc.update("INSERT INTO dbo.IntentoRecuperacion(TIPO,HUELLA,CREADO_EN) VALUES(?,?,"+ahora+"),(0,?,"+ahora+")",
                tipo,destino,ip);
            return destinoPrevio!=null && destinoPrevio<3 && ipPrevio!=null && ipPrevio<10;
        } finally { Arrays.fill(ip,(byte)0);Arrays.fill(destino,(byte)0); }
    }

    private void bloquear(byte tipo, byte[] huella) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getAutoCommit()) {
                throw new InvalidDataAccessApiUsageException("El límite de recuperación requiere una conexión transaccional.");
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, "SistemaFacturacionWeb.Recovery.rate." + tipo + "." + hex(huella));
                statement.execute();
            }
            return null;
        });
    }

    private static String hex(byte[] bytes) {
        var result=new StringBuilder(bytes.length*2);
        for(byte b:bytes) result.append(String.format("%02x",b));
        return result.toString();
    }
}
