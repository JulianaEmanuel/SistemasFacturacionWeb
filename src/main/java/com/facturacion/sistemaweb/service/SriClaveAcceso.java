package com.facturacion.sistemaweb.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Generador oficial de la Clave de Acceso (49 dígitos con Módulo 11) del SRI Ecuador. */
public final class SriClaveAcceso {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private SriClaveAcceso() {}

    public static String generar(
            LocalDate fechaEmision,
            String codDoc,
            String ruc,
            short ambiente,
            String establecimiento,
            String puntoEmision,
            String secuencial,
            String codigoNumerico,
            short tipoEmision) {
        String fecha = fechaEmision.format(FECHA_FMT);
        String doc = padLeft(soloDigitos(codDoc), 2);
        String rucNorm = padLeft(soloDigitos(ruc), 13);
        String amb = Short.toString(ambiente == 2 ? (short) 2 : (short) 1);
        String estab = padLeft(soloDigitos(establecimiento), 3);
        String pto = padLeft(soloDigitos(puntoEmision), 3);
        String sec = padLeft(soloDigitos(secuencial), 9);
        String codNum = padLeft(soloDigitos(codigoNumerico), 8);
        String emi = Short.toString(tipoEmision == 2 ? (short) 2 : (short) 1);

        String base48 = fecha + doc + rucNorm + amb + estab + pto + sec + codNum + emi;
        if (base48.length() != 48) {
            throw new IllegalStateException("La base de la clave de acceso debe tener 48 dígitos, tiene " + base48.length());
        }
        return base48 + modulo11(base48);
    }

    public static String secuencialDesdeId(Long idFactura) {
        long valor = idFactura == null || idFactura < 1 ? 1L : idFactura;
        String texto = Long.toString(valor);
        return texto.length() >= 9 ? texto.substring(texto.length() - 9) : padLeft(texto, 9);
    }

    public static String codigoNumericoDesdeId(Long idFactura) {
        long valor = idFactura == null ? 1L : Math.abs(idFactura);
        long codigo = (10000000L + (valor * 7919L)) % 99999999L;
        if (codigo == 0) {
            codigo = 12345678L;
        }
        return padLeft(Long.toString(codigo), 8);
    }

    /** Dígito verificador Módulo 11 (ponderadores 2..7 de derecha a izquierda). */
    public static int modulo11(String cadena48) {
        int factor = 2;
        int suma = 0;
        for (int i = cadena48.length() - 1; i >= 0; i--) {
            int digito = cadena48.charAt(i) - '0';
            suma += digito * factor;
            factor = (factor == 7) ? 2 : factor + 1;
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            return 0;
        }
        if (verificador == 10) {
            return 1;
        }
        return verificador;
    }

    private static String soloDigitos(String raw) {
        return raw == null ? "" : raw.replaceAll("[^0-9]", "");
    }

    private static String padLeft(String s, int length) {
        if (s.length() > length) {
            return s.substring(s.length() - length);
        }
        return "0".repeat(length - s.length()) + s;
    }
}
