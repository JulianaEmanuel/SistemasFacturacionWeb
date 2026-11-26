package com.facturacion.sistemaweb;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import com.facturacion.sistemaweb.repository.AdministracionRepository;

/** Solo estado de transacciones, SELECT y bloqueos temporales; nunca DML ni credenciales. */
@SpringBootTest @ActiveProfiles("postgres")
class BootstrapLockIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdministracionRepository repository;
    private boolean retenido() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory' AND granted)",
            Boolean.class));
    }
    @Test void conservaElBloqueoHastaFinalizarLaTransaccion() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            repository.bloquear();
            assertTrue(retenido());
            repository.totalUsuarios();
            assertTrue(retenido());
        });
        assertDisponible();
    }
    private void assertDisponible() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            assertEquals(Boolean.TRUE,jdbc.queryForObject(
                "SELECT pg_try_advisory_xact_lock(hashtext('SistemaFacturacionWeb.Admin'))",Boolean.class)));
    }
    @Test void exigeTransaccionExterna() {
        assertThrows(IllegalTransactionStateException.class,repository::bloquear);
    }
    @Test void dosTransaccionesSeExcluyenHastaFinalizarLaPrimera() throws Exception {
        var firstAcquired=new CountDownLatch(1);var releaseFirst=new CountDownLatch(1);
        var secondAttempt=new CountDownLatch(1);var secondAcquired=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                repository.bloquear();firstAcquired.countDown();
                try { if(!releaseFirst.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Espera de prueba agotada."); }
                catch(InterruptedException ex) { Thread.currentThread().interrupt();throw new IllegalStateException(ex); }
                status.setRollbackOnly();
            }));
            assertTrue(firstAcquired.await(5,TimeUnit.SECONDS));
            var second=pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                // Session consulta disponibilidad sin requerir una transacción física previa.
                assertEquals(Boolean.FALSE,jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(hashtext('SistemaFacturacionWeb.Admin'))",Boolean.class));
                secondAttempt.countDown();repository.bloquear();secondAcquired.countDown();
                assertTrue(retenido());status.setRollbackOnly();
            }));
            assertTrue(secondAttempt.await(5,TimeUnit.SECONDS));
            assertFalse(secondAcquired.await(200,TimeUnit.MILLISECONDS));
            releaseFirst.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            assertEquals(0,secondAcquired.getCount());assertDisponible();
        } finally { releaseFirst.countDown();pool.shutdownNow(); }
    }
}
