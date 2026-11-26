package com.facturacion.sistemaweb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import com.facturacion.sistemaweb.repository.AdministracionRepository;

class AdministracionRepositoryTest {
    @Test void elBloqueoRequiereTransaccionYUsaLaMismaConexion() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        var connection=mock(Connection.class);
        var statement=mock(PreparedStatement.class);
        when(jdbc.execute(any(ConnectionCallback.class)))
            .thenAnswer(i -> i.<ConnectionCallback<?>>getArgument(0).doInConnection(connection));
        when(connection.prepareStatement(contains("pg_advisory_xact_lock"))).thenReturn(statement);
        when(connection.getAutoCommit()).thenReturn(true,false);
        var repository=new AdministracionRepository(jdbc);

        assertThrows(InvalidDataAccessApiUsageException.class,repository::bloquear);
        verify(connection,never()).prepareStatement(anyString());
        repository.bloquear();
        verify(statement).execute();
        verify(statement).close();
        verify(connection,never()).commit();
    }

    @Test void insertaYRecuperaUnaSolaColumnaDeIdentidad() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        var connection=mock(Connection.class);
        var statement=mock(PreparedStatement.class);
        when(connection.prepareStatement(contains("INSERT INTO dbo.Usuario"),aryEq(new String[]{"id_usuario"})))
            .thenReturn(statement);
        when(jdbc.update(any(PreparedStatementCreator.class),any(KeyHolder.class))).thenAnswer(i -> {
            i.<PreparedStatementCreator>getArgument(0).createPreparedStatement(connection);
            i.<KeyHolder>getArgument(1).getKeyList().add(Map.of("id_usuario",44L));
            return 1;
        });

        assertEquals(44L,new AdministracionRepository(jdbc).insertarDevolviendoId("operador",2,true,new byte[]{1}));
        verify(statement).setString(1,"operador");
        verify(statement).setInt(2,2);
        verify(statement).setBoolean(3,true);
    }
}
