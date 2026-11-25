package com.facturacion.sistemaweb.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

/**
 * Cliente SOAP para los servicios web del SRI del Ecuador en ambiente de pruebas:
 * - RecepcionComprobantesOffline (validarComprobante)
 * - AutorizacionComprobantesOffline (autorizacionComprobante)
 */
@Service
public class SriClienteSoapService {

    public static final String URL_RECEPCION_PRUEBAS =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    public static final String URL_AUTORIZACION_PRUEBAS =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";

    public record RespuestaSri(
            String estado,
            boolean exitosaEnPruebas,
            String ambiente,
            String numeroAutorizacion,
            String fechaAutorizacion,
            String mensajeRetorno,
            String xmlRespuesta) {}

    private final HttpClient http;

    public SriClienteSoapService() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    public RespuestaSri enviarYConsultarAutorizacion(
            String xmlComprobante,
            String claveAcceso,
            String urlRecepcion,
            String urlAutorizacion,
            short ambienteCodigo) {
        String ambienteTexto = ambienteCodigo == 2 ? "PRODUCCIÓN" : "AMBIENTE DE PRUEBAS";
        String endpointRecepcion = normalizarEndpoint(urlRecepcion, URL_RECEPCION_PRUEBAS);
        String endpointAutorizacion = normalizarEndpoint(urlAutorizacion, URL_AUTORIZACION_PRUEBAS);

        try {
            String base64Xml = Base64.getEncoder().encodeToString(xmlComprobante.getBytes(StandardCharsets.UTF_8));
            String soapRecepcion = """
                    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.recepcion">
                      <soapenv:Header/>
                      <soapenv:Body>
                        <ec:validarComprobante>
                          <xml>%s</xml>
                        </ec:validarComprobante>
                      </soapenv:Body>
                    </soapenv:Envelope>
                    """.formatted(base64Xml);

            String respRecepcionXml = postSoap(endpointRecepcion, soapRecepcion);
            RespuestaParseada recepcion = parsearRespuesta(respRecepcionXml, false);

            if (!"RECIBIDA".equalsIgnoreCase(recepcion.estado())) {
                String estado = recepcion.estado().isBlank() ? "DEVUELTA" : recepcion.estado();
                String msg = "Envío NO exitoso bajo " + ambienteTexto + ". Estado SRI: " + estado
                        + (recepcion.detalle().isBlank() ? "" : " | Detalle SRI: " + recepcion.detalle());
                return new RespuestaSri(estado, false, ambienteTexto, null, null, msg, respRecepcionXml);
            }

            // Si fue RECIBIDA, consultar Autorización en el WS del SRI
            String soapAutorizacion = """
                    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ec="http://ec.gob.sri.ws.autorizacion">
                      <soapenv:Header/>
                      <soapenv:Body>
                        <ec:autorizacionComprobante>
                          <claveAccesoComprobante>%s</claveAccesoComprobante>
                        </ec:autorizacionComprobante>
                      </soapenv:Body>
                    </soapenv:Envelope>
                    """.formatted(SriXmlService.escaparXml(claveAcceso));

            String respAutoXml = postSoap(endpointAutorizacion, soapAutorizacion);
            RespuestaParseada auto = parsearRespuesta(respAutoXml, true);

            if ("AUTORIZADO".equalsIgnoreCase(auto.estado())) {
                String msg = "Envío EXITOSO bajo " + ambienteTexto + ". Comprobante AUTORIZADO por el SRI. "
                        + "Nro. Autorización: " + auto.numeroAutorizacion()
                        + (auto.fechaAutorizacion().isBlank() ? "" : " | Fecha: " + auto.fechaAutorizacion());
                return new RespuestaSri("AUTORIZADO", true, ambienteTexto, auto.numeroAutorizacion(),
                        auto.fechaAutorizacion(), msg, respAutoXml);
            }

            if (auto.estado().isBlank()) {
                String msg = "Envío EXITOSO (RECIBIDA) bajo " + ambienteTexto
                        + ". El SRI recibió el comprobante y quedó pendiente de autorización. Clave de acceso: " + claveAcceso;
                return new RespuestaSri("RECIBIDA", true, ambienteTexto, null, null, msg, respAutoXml);
            }

            String msg = "Comprobante RECIBIDO pero NO AUTORIZADO bajo " + ambienteTexto
                    + ". Estado SRI: " + auto.estado()
                    + (auto.detalle().isBlank() ? "" : " | Detalle SRI: " + auto.detalle());
            return new RespuestaSri(auto.estado(), false, ambienteTexto, auto.numeroAutorizacion(),
                    auto.fechaAutorizacion(), msg, respAutoXml);

        } catch (Exception ex) {
            String msg = "Error al conectar con el SRI bajo " + ambienteTexto + ": "
                    + ex.getClass().getSimpleName()
                    + (ex.getMessage() == null ? "" : " - " + ex.getMessage());
            return new RespuestaSri("ERROR_ENVIO", false, ambienteTexto, null, null, msg, null);
        }
    }

    protected String postSoap(String endpoint, String soapBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " desde SRI");
        }
        return response.body();
    }

    public record RespuestaParseada(String estado, String numeroAutorizacion, String fechaAutorizacion, String detalle) {}

    public static RespuestaParseada parsearRespuesta(String xml, boolean autorizacion) {
        if (xml == null || xml.isBlank()) {
            return new RespuestaParseada("", "", "", "");
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            String estado = textoElemento(doc, "estado");
            String numAuto = autorizacion ? textoElemento(doc, "numeroAutorizacion") : "";
            String fechaAuto = autorizacion ? textoElemento(doc, "fechaAutorizacion") : "";

            List<String> mensajes = new ArrayList<>();
            NodeList nodosMensaje = doc.getElementsByTagName("mensaje");
            for (int i = 0; i < nodosMensaje.getLength(); i++) {
                Node nodo = nodosMensaje.item(i);
                if (nodo instanceof Element el && el.getElementsByTagName("identificador").getLength() > 0) {
                    String id = textoHijo(el, "identificador");
                    String msg = textoHijo(el, "mensaje");
                    String info = textoHijo(el, "informacionAdicional");
                    String tipo = textoHijo(el, "tipo");
                    StringBuilder item = new StringBuilder();
                    if (!tipo.isBlank()) item.append("[").append(tipo).append("] ");
                    if (!id.isBlank()) item.append("Código ").append(id).append(": ");
                    item.append(msg);
                    if (!info.isBlank()) item.append(" (").append(info).append(")");
                    mensajes.add(item.toString());
                }
            }
            return new RespuestaParseada(estado, numAuto, fechaAuto, String.join(" ; ", mensajes));
        } catch (Exception ex) {
            return new RespuestaParseada("ERROR_PARSE", "", "", ex.getMessage());
        }
    }

    private static String textoElemento(Document doc, String nombre) {
        NodeList list = doc.getElementsByTagName(nombre);
        if (list.getLength() == 0) return "";
        String val = list.item(0).getTextContent();
        return val == null ? "" : val.strip();
    }

    private static String textoHijo(Element parent, String nombre) {
        NodeList list = parent.getElementsByTagName(nombre);
        if (list.getLength() == 0) return "";
        String val = list.item(0).getTextContent();
        return val == null ? "" : val.strip();
    }

    private static String normalizarEndpoint(String url, String fallback) {
        if (url == null || url.isBlank()) return fallback;
        String clean = url.strip();
        if (clean.endsWith("?wsdl") || clean.endsWith("?WSDL")) {
            return clean.substring(0, clean.length() - 5);
        }
        return clean;
    }
}
