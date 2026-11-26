package com.facturacion.sistemaweb;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.facturacion.sistemaweb.controller.FacturaSriController;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.security.AdminAccess;
import com.facturacion.sistemaweb.security.SecurityConfig;
import com.facturacion.sistemaweb.service.*;

@WebMvcTest(FacturaSriController.class)
@ActiveProfiles("postgres")
@Import({SecurityConfig.class, CapturaVistasConfig.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class SriFacturacionTest {

    @Autowired MockMvc mvc;
    @MockitoBean FacturaSriService sriService;
    @MockitoBean(name = "adminAccess") AdminAccess adminAccess;
    @MockitoBean UserDetailsService users;

    private EmisorSri emisorDemo() {
        EmisorSri e = new EmisorSri();
        e.setRuc("0999999999001");
        e.setRazonSocial("EMPRESA DEMO FACTURACION S.A.");
        e.setNombreComercial("SISTEMA FACTURACION WEB");
        e.setDirMatriz("Av. Quito y Calle 12, Quevedo");
        e.setDirEstablecimiento("Av. Quito y Calle 12, Quevedo");
        e.setCodEstablecimiento("001");
        e.setPuntoEmision("001");
        e.setAmbiente((short) 1);
        e.setTipoEmision((short) 1);
        e.setUrlRecepcion(SriClienteSoapService.URL_RECEPCION_PRUEBAS);
        e.setUrlAutorizacion(SriClienteSoapService.URL_AUTORIZACION_PRUEBAS);
        return e;
    }

    private FacturaVista facturaDemo() {
        var linea = new FacturaVista.Linea(
                5L, "Laptop Demo", new BigDecimal("2.00"), new BigDecimal("100.0000"),
                new BigDecimal("15.00"), new BigDecimal("200.0000"), new BigDecimal("30.0000"), new BigDecimal("230.0000"));
        return new FacturaVista(
                15L,
                LocalDateTime.of(2026, 11, 25, 12, 30),
                "0912345678",
                "Carlos Mendoza",
                "Ana Paz",
                new BigDecimal("200.0000"),
                new BigDecimal("30.0000"),
                new BigDecimal("230.0000"),
                List.of(linea),
                new FacturaVista.Contacto("Av. Principal", "0991112233", "carlos@example.invalid", true),
                new FacturaVista.Pago("Efectivo", new BigDecimal("230.0000")));
    }

    @Test
    void claveAccesoTiene49DigitosYModulo11Valido() {
        String clave = SriClaveAcceso.generar(
                LocalDate.of(2026, 11, 25), "01", "0999999999001", (short) 1,
                "001", "001", "000000015", "12345678", (short) 1);
        assertEquals(49, clave.length());
        assertTrue(clave.matches("[0-9]{49}"));
        int esperado = SriClaveAcceso.modulo11(clave.substring(0, 48));
        assertEquals(esperado, clave.charAt(48) - '0');
    }

    @Test
    void xmlSriContieneInfoTributariaInfoFacturaYDetallesDesdeBd() {
        SriXmlService xmlService = new SriXmlService();
        String clave = SriClaveAcceso.generar(
                LocalDate.of(2026, 11, 25), "01", "0999999999001", (short) 1,
                "001", "001", "000000015", "12345678", (short) 1);

        String xml = xmlService.generarXmlFactura(emisorDemo(), facturaDemo(), clave, "000000015");
        assertTrue(xml.contains("<factura id=\"comprobante\" version=\"1.1.0\">"));
        assertTrue(xml.contains("<ambiente>1</ambiente>"));
        assertTrue(xml.contains("<ruc>0999999999001</ruc>"));
        assertTrue(xml.contains("<claveAcceso>" + clave + "</claveAcceso>"));
        assertTrue(xml.contains("<tipoIdentificacionComprador>05</tipoIdentificacionComprador>"));
        assertTrue(xml.contains("<razonSocialComprador>Carlos Mendoza</razonSocialComprador>"));
        assertTrue(xml.contains("<codigoPorcentaje>4</codigoPorcentaje>"));
        assertTrue(xml.contains("<importeTotal>230.00</importeTotal>"));
        assertTrue(xml.contains("<descripcion>Laptop Demo</descripcion>"));
        assertTrue(xml.contains("<formaPago>01</formaPago>"));
    }

    @Test
    void ridePdfSeGeneraCorrectamenteConCodigoDeBarras() {
        SriRidePdfService rideService = new SriRidePdfService();
        EmisorSri emisor = emisorDemo();
        FacturaVista vista = facturaDemo();
        FacturaSri sri = new FacturaSri();
        sri.setIdFactura(15L);
        sri.setAmbiente((short) 1);
        sri.setEstablecimiento("001");
        sri.setPuntoEmision("001");
        sri.setSecuencial("000000015");
        sri.setClaveAcceso(SriClaveAcceso.generar(
                LocalDate.of(2026, 11, 25), "01", emisor.getRuc(), (short) 1,
                "001", "001", "000000015", "12345678", (short) 1));
        sri.setEstadoSri("AUTORIZADO");
        sri.setExitosoPruebas(true);
        sri.setMensajeRetorno("Envío EXITOSO bajo AMBIENTE DE PRUEBAS.");

        byte[] pdf = rideService.generarRide(emisor, vista, sri);
        assertTrue(pdf.length > 1000);
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
    }

    @Test
    void parserSoapSriInterpretaRespuestaDePruebasExitosayDevuelta() {
        String soapDevuelta = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <ns2:validarComprobanteResponse xmlns:ns2="http://ec.gob.sri.ws.recepcion">
                      <RespuestaRecepcionComprobante>
                        <estado>DEVUELTA</estado>
                        <comprobantes><comprobante><mensajes><mensaje>
                          <identificador>39</identificador>
                          <mensaje>FIRMA INVALIDA</mensaje>
                          <informacionAdicional>Certificado no reconocido</informacionAdicional>
                          <tipo>ERROR</tipo>
                        </mensaje></mensajes></comprobante></comprobantes>
                      </RespuestaRecepcionComprobante>
                    </ns2:validarComprobanteResponse>
                  </soap:Body>
                </soap:Envelope>
                """;
        var parsed = SriClienteSoapService.parsearRespuesta(soapDevuelta, false);
        assertEquals("DEVUELTA", parsed.estado());
        assertTrue(parsed.detalle().contains("39"));
        assertTrue(parsed.detalle().contains("FIRMA INVALIDA"));

        SriClienteSoapService stub = new SriClienteSoapService() {
            @Override
            protected String postSoap(String endpoint, String soapBody) {
                if (endpoint.contains("Recepcion")) {
                    return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><RespuestaRecepcionComprobante><estado>RECIBIDA</estado></RespuestaRecepcionComprobante></soap:Body></soap:Envelope>";
                }
                return """
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                          <soap:Body>
                            <autorizacion>
                              <estado>AUTORIZADO</estado>
                              <numeroAutorizacion>2511202601099999999900110010010000000151234567819</numeroAutorizacion>
                              <fechaAutorizacion>2026-11-25T12:35:00-05:00</fechaAutorizacion>
                            </autorizacion>
                          </soap:Body>
                        </soap:Envelope>
                        """;
            }
        };
        var res = stub.enviarYConsultarAutorizacion("<factura/>", "2511202601099999999900110010010000000151234567819", null, null, (short) 1);
        assertTrue(res.exitosaEnPruebas());
        assertEquals("AUTORIZADO", res.estado());
        assertTrue(res.mensajeRetorno().contains("AMBIENTE DE PRUEBAS"));
        assertTrue(res.mensajeRetorno().contains("EXITOSO"));
    }

    @Test
    void endpointsXmlRideEnvioYConfiguracionRespetanSeguridad() throws Exception {
        when(adminAccess.allowed(any())).thenAnswer(inv -> {
            Authentication auth = inv.getArgument(0);
            return auth != null && "admin".equals(auth.getName());
        });

        FacturaSri sri = new FacturaSri();
        sri.setIdFactura(15L);
        sri.setEstablecimiento("001");
        sri.setPuntoEmision("001");
        sri.setSecuencial("000000015");
        sri.setAmbiente((short) 1);
        sri.setClaveAcceso("2511202601099999999900110010010000000151234567819");
        sri.setEstadoSri("GENERADO");
        sri.setXmlComprobante("<?xml version=\"1.0\"?><factura id=\"comprobante\"/>");

        when(sriService.generarOActualizarXml(15L)).thenReturn(sri);
        when(sriService.generarRidePdf(15L)).thenReturn("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        when(sriService.enviarAlSriAmbientePruebas(15L)).thenReturn(
                new FacturaSriService.EnvioResultado(15L, sri.getClaveAcceso(), "AUTORIZADO", true, "AMBIENTE DE PRUEBAS", sri.getClaveAcceso(),
                        "Envío EXITOSO bajo AMBIENTE DE PRUEBAS. Comprobante AUTORIZADO."));
        when(sriService.cargarFormularioEmisor()).thenReturn(new EmisorSriForm());
        when(sriService.listarFacturasComoCliente()).thenReturn(List.of(facturaDemo()));

        mvc.perform(get("/modulos/facturas/15/xml")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/modulos/sri/configuracion").with(user("cliente"))).andExpect(status().isForbidden());
        mvc.perform(get("/modulos/sri/configuracion").with(user("admin"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Configuración del Emisor y Ambiente SRI")));

        mvc.perform(get("/modulos/facturas/15/xml").with(user("cliente")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"));

        mvc.perform(get("/modulos/facturas/15/ride").with(user("cliente")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"));

        mvc.perform(post("/modulos/facturas/15/enviar-sri").with(user("admin")).with(csrf()))
                .andExpect(redirectedUrl("/modulos/facturas/15"))
                .andExpect(flash().attribute("sriSuccess", containsString("AMBIENTE DE PRUEBAS")));

        mvc.perform(get("/modulos/mis-facturas").with(user("cliente")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BASE DE DATOS COMO CLIENTE")))
                .andExpect(content().string(containsString("RIDE PDF")));
    }
}
