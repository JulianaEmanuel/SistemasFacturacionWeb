package com.facturacion.sistemaweb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.facturacion.sistemaweb.security.CredencialVersionRepository;

class CredencialVersionRepositoryTest {
    @Test void permiteCompatibilidadAntesDeMigracionYRechazaSesionesAntiguasDespues() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.columns"),eq(Integer.class))).thenReturn(0);
        assertTrue(new CredencialVersionRepository(jdbc).sesionVigente(7L,Set.of("ACCOUNT_ID_7")));

        var migrado=mock(JdbcTemplate.class);
        when(migrado.queryForObject(contains("information_schema.columns"),eq(Integer.class))).thenReturn(1);
        when(migrado.queryForObject(contains("SELECT VERSION_CREDENCIAL"),eq(Long.class),eq(7L))).thenReturn(2L);
        var versiones=new CredencialVersionRepository(migrado);
        assertFalse(versiones.sesionVigente(7L,Set.of("ACCOUNT_ID_7")));
        assertTrue(versiones.sesionVigente(7L,Set.of("ACCOUNT_ID_7","CREDENTIAL_VERSION_2")));
        assertFalse(versiones.sesionVigente(7L,Set.of("ACCOUNT_ID_7","CREDENTIAL_VERSION_1")));
    }

    @Test void incrementarVersionRevocaLosTokensDeRecuperacionPendientes() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.columns"),eq(Integer.class))).thenReturn(1);
        when(jdbc.queryForObject(contains("to_regclass"),eq(Integer.class))).thenReturn(1);
        when(jdbc.update(anyString(),anyLong())).thenReturn(1);
        new CredencialVersionRepository(jdbc).incrementarYRevocar(18L);
        verify(jdbc).update(contains("VERSION_CREDENCIAL=COALESCE"),eq(18L));
        verify(jdbc).update(contains("UPDATE dbo.RecuperacionContrasenia SET REVOCADO_EN"),eq(18L));
    }
}
