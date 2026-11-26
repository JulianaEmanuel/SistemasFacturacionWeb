package com.facturacion.sistemaweb.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres")
public class PasePruebas {
    private static final long DURACION_SEGUNDOS = 30;
    private final byte[] clave;
    private final Map<String, Long> usados = new HashMap<>();

    public PasePruebas(@Value("${APP_PASE_PRUEBAS_CLAVE:}") String secreto) {
        clave = secreto.isBlank() ? new byte[0] : Base64.getDecoder().decode(secreto);
        if (clave.length != 0 && clave.length != 32) {
            throw new IllegalArgumentException("APP_PASE_PRUEBAS_CLAVE debe contener 32 bytes en Base64.");
        }
    }

    public boolean disponible() {
        return clave.length == 32;
    }

    public String emitir() {
        if (!disponible()) {
            throw new IllegalStateException("El acceso directo a pruebas no está configurado.");
        }
        String contenido = (Instant.now().getEpochSecond() + DURACION_SEGUNDOS) + "." + UUID.randomUUID();
        return contenido + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(firmar(contenido));
    }

    public synchronized boolean consumir(String pase) {
        if (!disponible() || pase == null || !pase.matches("[0-9]{1,12}\\.[a-f0-9-]{36}\\.[A-Za-z0-9_-]{43}")) {
            return false;
        }
        int primero = pase.indexOf('.');
        int ultimo = pase.lastIndexOf('.');
        long ahora = Instant.now().getEpochSecond();
        long vence = Long.parseLong(pase.substring(0, primero));
        if (vence <= ahora || vence > ahora + DURACION_SEGUNDOS) {
            return false;
        }
        if (!MessageDigest.isEqual(firmar(pase.substring(0, ultimo)),
                Base64.getUrlDecoder().decode(pase.substring(ultimo + 1)))) {
            return false;
        }
        usados.values().removeIf(expiracion -> expiracion <= ahora);
        return usados.size() < 10_000
                && usados.putIfAbsent(pase.substring(primero + 1, ultimo), vence) == null;
    }

    private byte[] firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(("facturacion-pruebas:" + contenido).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo firmar el pase de pruebas.", ex);
        }
    }
}
