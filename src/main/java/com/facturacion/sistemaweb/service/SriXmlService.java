package com.facturacion.sistemaweb.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import com.facturacion.sistemaweb.model.EmisorSri;
import com.facturacion.sistemaweb.model.FacturaVista;

/**
 * Construye el comprobante electrónico XML de Factura (versión 1.1.0)
 * según la ficha técnica del SRI del Ecuador a partir de encabezado, detalle,
 * datos administrativos del emisor y datos del cliente consultados desde la BD.
 */
@Service
public class SriXmlService {

    private static final DateTimeFormatter FECHA_SRI = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record ImpuestoAgrupado(String codigoPorcentaje, BigDecimal tarifa, BigDecimal baseImponible, BigDecimal valor) {}

    public String generarXmlFactura(EmisorSri emisor, FacturaVista factura, String claveAcceso, String secuencial) {
        StringBuilder xml = new StringBuilder(2048);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<factura id=\"comprobante\" version=\"1.1.0\">\n");

        // 1. Información Tributaria (Base de datos administrativa: EmisorSri)
        xml.append("  <infoTributaria>\n");
        tag(xml, 4, "ambiente", Short.toString(emisor.getAmbiente() != null && emisor.getAmbiente() == 2 ? (short) 2 : (short) 1));
        tag(xml, 4, "tipoEmision", Short.toString(emisor.getTipoEmision() != null ? emisor.getTipoEmision() : (short) 1));
        tag(xml, 4, "razonSocial", limpiar(emisor.getRazonSocial()));
        if (emisor.getNombreComercial() != null && !emisor.getNombreComercial().isBlank()) {
            tag(xml, 4, "nombreComercial", limpiar(emisor.getNombreComercial()));
        }
        tag(xml, 4, "ruc", limpiar(emisor.getRuc()));
        tag(xml, 4, "claveAcceso", claveAcceso);
        tag(xml, 4, "codDoc", "01");
        tag(xml, 4, "estab", emisor.getCodEstablecimiento());
        tag(xml, 4, "ptoEmi", emisor.getPuntoEmision());
        tag(xml, 4, "secuencial", secuencial);
        tag(xml, 4, "dirMatriz", limpiar(emisor.getDirMatriz()));
        xml.append("  </infoTributaria>\n");

        // 2. Información de Factura (Encabezado + Cliente desde BD)
        String identComprador = limpiarIdentificacion(factura.cedula());
        String tipoIdent = tipoIdentificacionSri(identComprador);
        List<ImpuestoAgrupado> impuestosTotales = agruparImpuestos(factura.lineas());
        BigDecimal totalSinImpuestos = sumSubtotal2(factura);
        BigDecimal importeTotal = sumTotal2(factura, impuestosTotales);

        xml.append("  <infoFactura>\n");
        tag(xml, 4, "fechaEmision", factura.fecha().toLocalDate().format(FECHA_SRI));
        tag(xml, 4, "dirEstablecimiento", limpiar(emisor.getDirEstablecimiento()));
        if (emisor.getContribuyenteEspecial() != null && !emisor.getContribuyenteEspecial().isBlank()) {
            tag(xml, 4, "contribuyenteEspecial", limpiar(emisor.getContribuyenteEspecial()));
        }
        tag(xml, 4, "obligadoContabilidad", emisor.isObligadoContabilidad() ? "SI" : "NO");
        tag(xml, 4, "tipoIdentificacionComprador", tipoIdent);
        tag(xml, 4, "razonSocialComprador", limpiar(factura.cliente()));
        tag(xml, 4, "identificacionComprador", identComprador);
        if (factura.contacto() != null && factura.contacto().direccion() != null && !factura.contacto().direccion().isBlank()) {
            tag(xml, 4, "direccionComprador", limpiar(factura.contacto().direccion()));
        }
        tag(xml, 4, "totalSinImpuestos", fmt2(totalSinImpuestos));
        tag(xml, 4, "totalDescuento", "0.00");

        xml.append("    <totalConImpuestos>\n");
        for (ImpuestoAgrupado imp : impuestosTotales) {
            xml.append("      <totalImpuesto>\n");
            tag(xml, 8, "codigo", "2");
            tag(xml, 8, "codigoPorcentaje", imp.codigoPorcentaje());
            tag(xml, 8, "baseImponible", fmt2(imp.baseImponible()));
            tag(xml, 8, "tarifa", fmt2(imp.tarifa()));
            tag(xml, 8, "valor", fmt2(imp.valor()));
            xml.append("      </totalImpuesto>\n");
        }
        xml.append("    </totalConImpuestos>\n");

        tag(xml, 4, "propina", "0.00");
        tag(xml, 4, "importeTotal", fmt2(importeTotal));
        tag(xml, 4, "moneda", "DOLAR");

        xml.append("    <pagos>\n");
        xml.append("      <pago>\n");
        tag(xml, 8, "formaPago", codigoFormaPagoSri(factura.pago()));
        tag(xml, 8, "total", fmt2(importeTotal));
        xml.append("      </pago>\n");
        xml.append("    </pagos>\n");
        xml.append("  </infoFactura>\n");

        // 3. Detalles de la Factura (Líneas desde Detallefactura + Producto)
        xml.append("  <detalles>\n");
        for (FacturaVista.Linea linea : factura.lineas()) {
            BigDecimal base = linea.subtotal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaVal = linea.iva().setScale(2, RoundingMode.HALF_UP);
            String codPorc = codigoPorcentajeIva(linea.porcentaje());

            xml.append("    <detalle>\n");
            tag(xml, 6, "codigoPrincipal", "PROD-" + linea.idProducto());
            tag(xml, 6, "descripcion", limpiar(linea.nombre()));
            tag(xml, 6, "cantidad", linea.cantidad().setScale(2, RoundingMode.HALF_UP).toPlainString());
            tag(xml, 6, "precioUnitario", linea.precio().setScale(4, RoundingMode.HALF_UP).toPlainString());
            tag(xml, 6, "descuento", "0.00");
            tag(xml, 6, "precioTotalSinImpuesto", fmt2(base));
            xml.append("      <impuestos>\n");
            xml.append("        <impuesto>\n");
            tag(xml, 10, "codigo", "2");
            tag(xml, 10, "codigoPorcentaje", codPorc);
            tag(xml, 10, "tarifa", fmt2(linea.porcentaje()));
            tag(xml, 10, "baseImponible", fmt2(base));
            tag(xml, 10, "valor", fmt2(ivaVal));
            xml.append("        </impuesto>\n");
            xml.append("      </impuestos>\n");
            xml.append("    </detalle>\n");
        }
        xml.append("  </detalles>\n");

        // 4. Información Adicional (Correo, Teléfono, Empleado, Ambiente)
        List<String[]> adicionales = new ArrayList<>();
        if (factura.contacto() != null && factura.contacto().correo() != null && !factura.contacto().correo().isBlank()) {
            adicionales.add(new String[]{"Email", limpiar(factura.contacto().correo())});
        }
        if (factura.contacto() != null && factura.contacto().telefono() != null && !factura.contacto().telefono().isBlank()) {
            adicionales.add(new String[]{"Teléfono", limpiar(factura.contacto().telefono())});
        }
        if (factura.empleado() != null && !factura.empleado().isBlank()) {
            adicionales.add(new String[]{"AtendidoPor", limpiar(factura.empleado())});
        }
        adicionales.add(new String[]{"Ambiente", emisor.ambienteEtiqueta()});

        if (!adicionales.isEmpty()) {
            xml.append("  <infoAdicional>\n");
            for (String[] campo : adicionales) {
                xml.append("    <campoAdicional nombre=\"").append(escaparXml(campo[0])).append("\">")
                   .append(escaparXml(campo[1])).append("</campoAdicional>\n");
            }
            xml.append("  </infoAdicional>\n");
        }

        xml.append("</factura>\n");
        return xml.toString();
    }

    public List<ImpuestoAgrupado> agruparImpuestos(List<FacturaVista.Linea> lineas) {
        Map<String, BigDecimal[]> grupos = new LinkedHashMap<>();
        for (FacturaVista.Linea linea : lineas) {
            String codPorc = codigoPorcentajeIva(linea.porcentaje());
            BigDecimal base = linea.subtotal().setScale(2, RoundingMode.HALF_UP);
            BigDecimal val = linea.iva().setScale(2, RoundingMode.HALF_UP);
            grupos.compute(codPorc, (k, acc) -> {
                if (acc == null) {
                    return new BigDecimal[]{linea.porcentaje(), base, val};
                }
                acc[1] = acc[1].add(base);
                acc[2] = acc[2].add(val);
                return acc;
            });
        }
        if (grupos.isEmpty()) {
            return List.of(new ImpuestoAgrupado("0", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        List<ImpuestoAgrupado> res = new ArrayList<>();
        for (var e : grupos.entrySet()) {
            res.add(new ImpuestoAgrupado(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
        }
        return res;
    }

    public static String codigoPorcentajeIva(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) == 0) return "0";
        BigDecimal p = porcentaje.stripTrailingZeros();
        if (p.compareTo(new BigDecimal("12")) == 0) return "2";
        if (p.compareTo(new BigDecimal("14")) == 0) return "3";
        if (p.compareTo(new BigDecimal("15")) == 0) return "4";
        if (p.compareTo(new BigDecimal("5")) == 0) return "5";
        if (p.compareTo(new BigDecimal("13")) == 0) return "10";
        return "4";
    }

    public static String tipoIdentificacionSri(String identificacion) {
        if ("9999999999999".equals(identificacion)) return "07";
        if (identificacion.matches("[0-9]{13}")) return "04";
        if (identificacion.matches("[0-9]{10}")) return "05";
        return "06";
    }

    public static String codigoFormaPagoSri(FacturaVista.Pago pago) {
        if (pago == null || pago.nombre() == null) return "01";
        String n = pago.nombre().toLowerCase(Locale.ROOT);
        if (n.contains("crédito") || n.contains("credito")) return "19";
        if (n.contains("débito") || n.contains("debito")) return "16";
        if (n.contains("transferencia")) return "20";
        return "01";
    }

    private BigDecimal sumSubtotal2(FacturaVista f) {
        if (f.lineas().isEmpty()) return f.subtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = BigDecimal.ZERO;
        for (var l : f.lineas()) {
            sum = sum.add(l.subtotal().setScale(2, RoundingMode.HALF_UP));
        }
        return sum;
    }

    private BigDecimal sumTotal2(FacturaVista f, List<ImpuestoAgrupado> impuestos) {
        BigDecimal sub = sumSubtotal2(f);
        BigDecimal iva = BigDecimal.ZERO;
        for (var imp : impuestos) {
            iva = iva.add(imp.valor());
        }
        return sub.add(iva);
    }

    private static String fmt2(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String limpiarIdentificacion(String raw) {
        if (raw == null || raw.isBlank() || "—".equals(raw.strip())) {
            return "9999999999999";
        }
        return raw.strip();
    }

    private static String limpiar(String raw) {
        if (raw == null || raw.isBlank()) return "NO REGISTRADO";
        return raw.strip().replaceAll("\\s+", " ");
    }

    private static void tag(StringBuilder sb, int indent, String nombre, String valor) {
        sb.append(" ".repeat(indent)).append("<").append(nombre).append(">")
          .append(escaparXml(valor))
          .append("</").append(nombre).append(">\n");
    }

    public static String escaparXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
