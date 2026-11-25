package com.facturacion.sistemaweb.service;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.springframework.stereotype.Service;
import com.facturacion.sistemaweb.model.*;

/**
 * Genera el RIDE (Representación Impresa de Documento Electrónico) en PDF
 * conforme al estándar del SRI Ecuador, usando datos consultados de la BD.
 */
@Service
public class SriRidePdfService {

    private static final Color VERDE = new Color(30, 107, 58);
    private static final Color VERDE_CLARO = new Color(235, 244, 238);
    private static final Color TEXTO = new Color(25, 32, 40);
    private static final Color GRIS = new Color(90, 102, 114);
    private static final Color BORDE = new Color(185, 194, 202);
    private static final Color ZEBRA = new Color(247, 249, 250);
    private static final float MARGEN = 32f;
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Patrones oficiales Code 128 (0..106)
    private static final String[] CODE128 = {
        "212222","222122","222221","121223","121322","131222","122213","122312","132212","221213",
        "221312","231212","112232","122132","122231","113222","123122","123221","223211","221132",
        "221231","213212","223112","312131","311222","321122","321221","312212","322112","322211",
        "212123","212321","232121","111323","131123","131321","112313","132113","132311","211313",
        "231113","231311","112133","112331","132131","113123","113321","133121","313121","211331",
        "231131","213113","213311","213131","311123","311321","331121","312113","312311","332111",
        "314111","221411","431111","111224","111422","121124","121421","141122","141221","112214",
        "112412","122114","122411","142112","142211","241211","221114","413111","241112","134111",
        "111242","121142","121241","114212","124112","124211","411212","421112","421211","212141",
        "214121","412121","111143","111341","131141","114113","114311","411113","411311","113141",
        "114131","311141","411131","211412","211214","211232","2331112"
    };

    public byte[] generarRide(EmisorSri emisor, FacturaVista factura, FacturaSri sri) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = cargarFuente(doc);
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float ancho = page.getMediaBox().getWidth();
            float alto = page.getMediaBox().getHeight();
            float anchoUtil = ancho - 2 * MARGEN; // ~531 pt

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float yTop = alto - MARGEN;

                // --- BLOQUE SUPERIOR: 2 COLUMNAS (EMISOR IZQUIERDA | INFO TRIBUTARIA + BARRAS DERECHA) ---
                float colGap = 12f;
                float leftW = 248f;
                float rightW = anchoUtil - leftW - colGap;
                float headerH = 212f;
                float leftX = MARGEN;
                float rightX = MARGEN + leftW + colGap;
                float headerBottom = yTop - headerH;

                // Caja Emisor (izquierda)
                float logoH = 52f;
                rectFill(cs, leftX, yTop - logoH, leftW, logoH, VERDE);
                negrita(cs, font, 12.5f, leftX + 10, yTop - 23, cortar(emisor.getNombreComercial() != null && !emisor.getNombreComercial().isBlank() ? emisor.getNombreComercial() : emisor.getRazonSocial(), 28), Color.WHITE);
                texto(cs, font, 8f, leftX + 10, yTop - 39, "COMPROBANTE ELECTRÓNICO · SRI ECUADOR", Color.WHITE);

                float emisorBoxH = headerH - logoH - 6f;
                rectStroke(cs, leftX, headerBottom, leftW, emisorBoxH, BORDE);
                float ey = headerBottom + emisorBoxH - 16f;
                negrita(cs, font, 9f, leftX + 8, ey, cortar(emisor.getRazonSocial(), 42), TEXTO);
                ey -= 15f;
                ey = parrafoEtiqueta(cs, font, leftX + 8, ey, leftW - 16, "Dir. Matriz:", emisor.getDirMatriz());
                ey -= 4f;
                ey = parrafoEtiqueta(cs, font, leftX + 8, ey, leftW - 16, "Dir. Sucursal:", emisor.getDirEstablecimiento());
                ey -= 4f;
                if (emisor.getContribuyenteEspecial() != null && !emisor.getContribuyenteEspecial().isBlank()) {
                    ey = parrafoEtiqueta(cs, font, leftX + 8, ey, leftW - 16, "Contribuyente Especial Nro.:", emisor.getContribuyenteEspecial());
                    ey -= 4f;
                }
                parrafoEtiqueta(cs, font, leftX + 8, ey, leftW - 16, "OBLIGADO A LLEVAR CONTABILIDAD:", emisor.isObligadoContabilidad() ? "SI" : "NO");

                // Caja Tributaria (derecha)
                rectStroke(cs, rightX, headerBottom, rightW, headerH, BORDE);
                float ry = yTop - 16f;
                negrita(cs, font, 10f, rightX + 10, ry, "R.U.C.: " + emisor.getRuc(), TEXTO);
                ry -= 18f;
                rectFill(cs, rightX + 10, ry - 5, rightW - 20, 18, VERDE_CLARO);
                negrita(cs, font, 11f, rightX + 14, ry, "FACTURA", VERDE);
                ry -= 18f;
                negrita(cs, font, 9.5f, rightX + 10, ry, "No. " + sri.numeroComprobante(), TEXTO);
                ry -= 16f;
                negrita(cs, font, 7.5f, rightX + 10, ry, "NÚMERO DE AUTORIZACIÓN:", GRIS);
                ry -= 11f;
                String numAuto = sri.getNumeroAutorizacion() != null && !sri.getNumeroAutorizacion().isBlank()
                        ? sri.getNumeroAutorizacion() : sri.getClaveAcceso();
                texto(cs, font, 7.2f, rightX + 10, ry, numAuto, TEXTO);
                ry -= 14f;
                negrita(cs, font, 7.5f, rightX + 10, ry, "FECHA Y HORA DE AUTORIZACIÓN:", GRIS);
                ry -= 11f;
                String fechaAuto = sri.getFechaAutorizacion() != null
                        ? sri.getFechaAutorizacion().format(FECHA_HORA_FMT)
                        : (sri.getFechaEnvio() != null ? sri.getFechaEnvio().format(FECHA_HORA_FMT) + " (Envío)" : "PENDIENTE");
                texto(cs, font, 8f, rightX + 10, ry, fechaAuto, TEXTO);
                ry -= 14f;
                negrita(cs, font, 8f, rightX + 10, ry, "AMBIENTE: " + sri.ambienteTexto(), VERDE);
                texto(cs, font, 8f, rightX + 125, ry, "EMISIÓN: NORMAL", TEXTO);
                ry -= 14f;
                negrita(cs, font, 7.5f, rightX + 10, ry, "CLAVE DE ACCESO:", GRIS);
                ry -= 32f;
                dibujarCode128(cs, sri.getClaveAcceso(), rightX + 10, ry, rightW - 20, 26f);
                ry -= 10f;
                texto(cs, font, 7.1f, rightX + 10, ry, sri.getClaveAcceso(), TEXTO);

                // --- BLOQUE COMPRADOR ---
                float y = headerBottom - 10f;
                float buyerH = 66f;
                rectStroke(cs, MARGEN, y - buyerH, anchoUtil, buyerH, BORDE);
                float by = y - 14f;
                negrita(cs, font, 8.2f, MARGEN + 8, by, "Razón Social / Nombres y Apellidos:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 165, by, cortar(factura.cliente(), 58), TEXTO);
                by -= 14f;
                negrita(cs, font, 8.2f, MARGEN + 8, by, "Identificación:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 75, by, factura.cedula(), TEXTO);
                negrita(cs, font, 8.2f, MARGEN + 260, by, "Fecha Emisión:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 330, by, factura.fecha().format(FECHA_FMT), TEXTO);
                by -= 14f;
                String dirCli = factura.contacto() != null && factura.contacto().direccion() != null ? factura.contacto().direccion() : "No registrada";
                negrita(cs, font, 8.2f, MARGEN + 8, by, "Dirección:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 58, by, cortar(dirCli, 68), TEXTO);
                by -= 14f;
                String correoCli = factura.contacto() != null && factura.contacto().correo() != null ? factura.contacto().correo() : "—";
                negrita(cs, font, 8.2f, MARGEN + 8, by, "Correo:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 48, by, cortar(correoCli, 38), TEXTO);
                negrita(cs, font, 8.2f, MARGEN + 260, by, "Factura Interna #:", TEXTO);
                texto(cs, font, 8.2f, MARGEN + 345, by, Long.toString(factura.id()), TEXTO);

                // --- TABLA DE DETALLES ---
                y = y - buyerH - 12f;
                float[] colW = {66f, 48f, 213f, 68f, 56f, 80f};
                String[] headers = {"Cod. Principal", "Cant.", "Descripción", "Precio Unit.", "IVA %", "Precio Total"};
                float rowH = 19f;
                rectFill(cs, MARGEN, y - rowH, anchoUtil, rowH, VERDE);
                float cx = MARGEN;
                for (int i = 0; i < headers.length; i++) {
                    if (i >= 3 || i == 1) {
                        derecha(cs, font, 8f, cx + colW[i] - 5, y - 13, headers[i], Color.WHITE);
                    } else {
                        negrita(cs, font, 8f, cx + 5, y - 13, headers[i], Color.WHITE);
                    }
                    cx += colW[i];
                }
                y -= rowH;

                int idx = 0;
                for (FacturaVista.Linea l : factura.lineas()) {
                    if (y < 200f) {
                        break; // Protección simple de página
                    }
                    if (idx++ % 2 == 1) {
                        rectFill(cs, MARGEN, y - rowH, anchoUtil, rowH, ZEBRA);
                    }
                    rectStroke(cs, MARGEN, y - rowH, anchoUtil, rowH, BORDE);
                    cx = MARGEN;
                    texto(cs, font, 8f, cx + 5, y - 13, "PROD-" + l.idProducto(), TEXTO);
                    cx += colW[0];
                    derecha(cs, font, 8f, cx + colW[1] - 5, y - 13, l.cantidadVisible(), TEXTO);
                    cx += colW[1];
                    texto(cs, font, 8f, cx + 5, y - 13, cortar(l.nombre(), 40), TEXTO);
                    cx += colW[2];
                    derecha(cs, font, 8f, cx + colW[3] - 5, y - 13, FacturaVista.dinero(l.precio()), TEXTO);
                    cx += colW[3];
                    derecha(cs, font, 8f, cx + colW[4] - 5, y - 13, l.porcentajeVisible(), TEXTO);
                    cx += colW[4];
                    derecha(cs, font, 8f, cx + colW[5] - 5, y - 13, FacturaVista.dinero(l.subtotal()), TEXTO);
                    y -= rowH;
                }

                // --- BLOQUE INFERIOR: INFO ADICIONAL + FORMAS DE PAGO (IZQ) | TOTALES (DER) ---
                y -= 14f;
                float footLeftW = 305f;
                float footRightW = anchoUtil - footLeftW - 12f;
                float footRightX = MARGEN + footLeftW + 12f;

                // Totales (derecha)
                BigDecimal base15 = BigDecimal.ZERO;
                BigDecimal base5 = BigDecimal.ZERO;
                BigDecimal base0 = BigDecimal.ZERO;
                BigDecimal baseOtros = BigDecimal.ZERO;
                for (FacturaVista.Linea l : factura.lineas()) {
                    BigDecimal sub = l.subtotal().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal pct = l.porcentaje().stripTrailingZeros();
                    if (pct.compareTo(BigDecimal.ZERO) == 0) base0 = base0.add(sub);
                    else if (pct.compareTo(new BigDecimal("15")) == 0) base15 = base15.add(sub);
                    else if (pct.compareTo(new BigDecimal("5")) == 0) base5 = base5.add(sub);
                    else baseOtros = baseOtros.add(sub);
                }
                BigDecimal subSinImp = factura.subtotal().setScale(2, RoundingMode.HALF_UP);
                BigDecimal ivaTot = factura.iva().setScale(2, RoundingMode.HALF_UP);
                BigDecimal valTotal = factura.total().setScale(2, RoundingMode.HALF_UP);

                String[][] totalesRows = {
                    {"SUBTOTAL 15%", FacturaVista.dinero(base15)},
                    {"SUBTOTAL 5%", FacturaVista.dinero(base5)},
                    {"SUBTOTAL 0%", FacturaVista.dinero(base0)},
                    {"SUBTOTAL OTROS IVA", FacturaVista.dinero(baseOtros)},
                    {"SUBTOTAL SIN IMPUESTOS", FacturaVista.dinero(subSinImp)},
                    {"TOTAL DESCUENTO", "0.00"},
                    {"IVA", FacturaVista.dinero(ivaTot)},
                    {"PROPINA", "0.00"},
                    {"VALOR TOTAL", FacturaVista.dinero(valTotal)}
                };

                float ty = y;
                for (int i = 0; i < totalesRows.length; i++) {
                    boolean ultimo = (i == totalesRows.length - 1);
                    float th = ultimo ? 20f : 16f;
                    if (ultimo) {
                        rectFill(cs, footRightX, ty - th, footRightW, th, VERDE_CLARO);
                    }
                    rectStroke(cs, footRightX, ty - th, footRightW, th, BORDE);
                    if (ultimo) {
                        negrita(cs, font, 8.5f, footRightX + 6, ty - 13, totalesRows[i][0], VERDE);
                        negrita(cs, font, 9f, footRightX + footRightW - 6 - ancho(font, 9f, "$ " + totalesRows[i][1]), ty - 13, "$ " + totalesRows[i][1], VERDE);
                    } else {
                        texto(cs, font, 7.6f, footRightX + 6, ty - 11.5f, totalesRows[i][0], TEXTO);
                        derecha(cs, font, 7.8f, footRightX + footRightW - 6, ty - 11.5f, totalesRows[i][1], TEXTO);
                    }
                    ty -= th;
                }

                // Info Adicional + Forma de Pago (izquierda)
                float iy = y;
                float infoH = 84f;
                rectStroke(cs, MARGEN, iy - infoH, footLeftW, infoH, BORDE);
                rectFill(cs, MARGEN, iy - 16f, footLeftW, 16f, VERDE_CLARO);
                negrita(cs, font, 8f, MARGEN + 6, iy - 11.5f, "INFORMACIÓN ADICIONAL", VERDE);
                float lineY = iy - 28f;
                lineY = parrafoEtiqueta(cs, font, MARGEN + 6, lineY, footLeftW - 12, "Email:", correoCli);
                String telCli = factura.contacto() != null && factura.contacto().telefono() != null ? factura.contacto().telefono() : "No registrado";
                lineY = parrafoEtiqueta(cs, font, MARGEN + 6, lineY - 2, footLeftW - 12, "Teléfono:", telCli);
                lineY = parrafoEtiqueta(cs, font, MARGEN + 6, lineY - 2, footLeftW - 12, "Atendido por:", factura.empleado());
                parrafoEtiqueta(cs, font, MARGEN + 6, lineY - 2, footLeftW - 12, "Estado SRI (" + sri.ambienteTexto() + "):", sri.getEstadoSri());

                float py = iy - infoH - 8f;
                float pagoH = 52f;
                rectStroke(cs, MARGEN, py - pagoH, footLeftW, pagoH, BORDE);
                rectFill(cs, MARGEN, py - 16f, footLeftW, 16f, VERDE_CLARO);
                negrita(cs, font, 8f, MARGEN + 6, py - 11.5f, "FORMAS DE PAGO (CATÁLOGO SRI)", VERDE);
                String codPagoSri = SriXmlService.codigoFormaPagoSri(factura.pago());
                String nomPago = factura.pago() != null ? factura.pago().nombre() : "Sin utilización del sistema financiero";
                texto(cs, font, 8f, MARGEN + 6, py - 29f, codPagoSri + " - " + cortar(nomPago, 36), TEXTO);
                derecha(cs, font, 8f, MARGEN + footLeftW - 6, py - 29f, "$ " + FacturaVista.dinero(valTotal), TEXTO);
                if (factura.pago() != null && !factura.pago().detalleVisible().isBlank()) {
                    texto(cs, font, 7.2f, MARGEN + 6, py - 42f, cortar(factura.pago().detalleVisible(), 54), GRIS);
                }

                // Pie de página
                if (sri.getMensajeRetorno() != null && !sri.getMensajeRetorno().isBlank()) {
                    texto(cs, font, 7.2f, MARGEN, 40f, "Retorno SRI: " + cortar(sri.getMensajeRetorno(), 110), GRIS);
                }
                texto(cs, font, 7.2f, MARGEN, 26f, "RIDE generado desde base de datos · Sistema de Facturación Web · Ambiente: " + sri.ambienteTexto(), GRIS);
            }

            doc.getDocumentInformation().setTitle("RIDE Factura " + sri.numeroComprobante());
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new FacturaException("No se pudo generar el PDF RIDE de la factura.");
        }
    }

    private static PDFont cargarFuente(PDDocument doc) throws IOException {
        try (InputStream fuente = SriRidePdfService.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
            return fuente == null ? new PDType1Font(Standard14Fonts.FontName.HELVETICA) : PDType0Font.load(doc, fuente, true);
        }
    }

    private static void dibujarCode128(PDPageContentStream cs, String text, float x, float y, float maxW, float h) throws IOException {
        if (text == null || text.isEmpty()) return;
        List<Integer> codes = new ArrayList<>();
        codes.add(104); // Start B
        int checksum = 104;
        for (int i = 0; i < text.length(); i++) {
            int val = text.charAt(i) - 32;
            if (val < 0 || val > 95) val = 16; // fallback '0'
            codes.add(val);
            checksum += (i + 1) * val;
        }
        codes.add(checksum % 103);
        codes.add(106); // Stop

        int totalModules = 0;
        for (int c : codes) {
            String pat = CODE128[c];
            for (int k = 0; k < pat.length(); k++) {
                totalModules += (pat.charAt(k) - '0');
            }
        }
        float modW = maxW / Math.max(1, totalModules);
        float curX = x;
        cs.setNonStrokingColor(Color.BLACK);
        for (int c : codes) {
            String pat = CODE128[c];
            for (int k = 0; k < pat.length(); k++) {
                float barW = (pat.charAt(k) - '0') * modW;
                if (k % 2 == 0) {
                    cs.addRect(curX, y, barW, h);
                    cs.fill();
                }
                curX += barW;
            }
        }
    }

    private static float parrafoEtiqueta(PDPageContentStream cs, PDFont font, float x, float y, float maxW, String label, String val) throws IOException {
        String full = label + " " + (val == null || val.isBlank() ? "—" : val.strip());
        List<String> lines = partir(font, full, maxW, 7.6f);
        float curY = y;
        for (String line : lines) {
            texto(cs, font, 7.6f, x, curY, line, TEXTO);
            curY -= 10f;
        }
        return curY;
    }

    private static List<String> partir(PDFont font, String raw, float maxW, float size) throws IOException {
        String text = seguro(font, raw);
        List<String> lines = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (int cp : text.codePoints().toArray()) {
            String next = new String(Character.toChars(cp));
            if (actual.length() > 0 && font.getStringWidth(actual + next) * size / 1000f > maxW) {
                lines.add(actual.toString());
                actual.setLength(0);
            }
            actual.append(next);
        }
        if (actual.length() > 0) lines.add(actual.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static String seguro(PDFont font, String raw) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int cp : (raw == null ? "" : raw).codePoints().toArray()) {
            if (Character.isISOControl(cp)) { out.append(' '); continue; }
            String text = new String(Character.toChars(cp));
            try { font.encode(text); out.append(text); } catch (IllegalArgumentException ex) { out.append('?'); }
        }
        return out.toString();
    }

    private static float ancho(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(seguro(font, text)) * size / 1000f;
    }

    private static void texto(PDPageContentStream cs, PDFont font, float size, float x, float y, String text, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(seguro(font, text));
        cs.endText();
    }

    private static void negrita(PDPageContentStream cs, PDFont font, float size, float x, float y, String text, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.setStrokingColor(color);
        cs.setLineWidth(size * 0.03f);
        cs.beginText();
        cs.setRenderingMode(RenderingMode.FILL_STROKE);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(seguro(font, text));
        cs.setRenderingMode(RenderingMode.FILL);
        cs.endText();
    }

    private static void derecha(PDPageContentStream cs, PDFont font, float size, float xDerecho, float y, String text, Color color) throws IOException {
        texto(cs, font, size, xDerecho - ancho(font, size, text), y, text, color);
    }

    private static void rectFill(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void rectStroke(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.75f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static String cortar(String s, int max) {
        if (s == null) return "";
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
