package com.facturacion.sistemaweb.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.repository.*;

/**
 * Orquesta la facturación electrónica SRI:
 * 1) Consulta la base administrativa (EmisorSri, Empleado/Usuario, FacturaSri).
 * 2) Consulta la base de cliente y encabezado/detalle de factura (Factura, Detallefactura, FacturaCliente/Persona, FacturaPago).
 * 3) Genera XML SRI y PDF RIDE.
 * 4) Envía el XML al SRI en ambiente de pruebas y guarda el retorno explícito.
 */
@Service
@Profile({"sqlserver", "postgres"})
@PreAuthorize("isAuthenticated()")
public class FacturaSriService {

    public record EnvioResultado(
            Long idFactura,
            String claveAcceso,
            String estadoSri,
            boolean exitosaEnPruebas,
            String ambiente,
            String numeroAutorizacion,
            String mensajeRetorno) {}

    private final EmisorSriRepository emisores;
    private final FacturaSriRepository facturasSri;
    private final FacturaService facturaService;
    private final FacturaRepository facturaRepository;
    private final DetalleFacturaRepository detalleRepository;
    private final PersonaRepository personaRepository;
    private final FacturaClienteRepository historicos;
    private final FacturaPagoRepository pagos;
    private final EmpleadoSesionService sesion;
    private final SriXmlService xmlService;
    private final SriFirmaXmlService firmaService;
    @org.springframework.beans.factory.annotation.Value("#{environment.matchesProfiles('pruebas')}")
    private boolean ambientePruebas;

    private final SriClienteSoapService soapService;
    private final SriRidePdfService rideService;
    private final JdbcTemplate jdbc;

    public FacturaSriService(
            EmisorSriRepository emisores,
            FacturaSriRepository facturasSri,
            FacturaService facturaService,
            FacturaRepository facturaRepository,
            DetalleFacturaRepository detalleRepository,
            PersonaRepository personaRepository,
            FacturaClienteRepository historicos,
            FacturaPagoRepository pagos,
            EmpleadoSesionService sesion,
            SriXmlService xmlService,
            SriFirmaXmlService firmaService,
            SriClienteSoapService soapService,
            SriRidePdfService rideService,
            JdbcTemplate jdbc) {
        this.emisores = emisores;
        this.facturasSri = facturasSri;
        this.facturaService = facturaService;
        this.facturaRepository = facturaRepository;
        this.detalleRepository = detalleRepository;
        this.personaRepository = personaRepository;
        this.historicos = historicos;
        this.pagos = pagos;
        this.sesion = sesion;
        this.xmlService = xmlService;
        this.firmaService = firmaService;
        this.soapService = soapService;
        this.rideService = rideService;
        this.jdbc = jdbc;
    }

    @Transactional
    public EmisorSri obtenerEmisorActivo() {
        EmisorSri emisor = emisores.findFirstByActivoTrueOrderByIdEmisorAsc().orElseGet(() -> {
            EmisorSri def = new EmisorSri();
            def.setRuc("0999999999001");
            def.setRazonSocial("EMPRESA DEMO FACTURACION S.A.");
            def.setNombreComercial("SISTEMA FACTURACION WEB");
            def.setDirMatriz("Av. Quito y Calle 12, Quevedo, Los Rios");
            def.setDirEstablecimiento("Av. Quito y Calle 12, Quevedo, Los Rios");
            def.setCodEstablecimiento("001");
            def.setPuntoEmision("001");
            def.setObligadoContabilidad(false);
            def.setAmbiente((short) 1);
            def.setTipoEmision((short) 1);
            def.setUrlRecepcion(SriClienteSoapService.URL_RECEPCION_PRUEBAS);
            def.setUrlAutorizacion(SriClienteSoapService.URL_AUTORIZACION_PRUEBAS);
            def.setActivo(true);
            return emisores.save(def);
        });
        emisor.setAmbiente((short) 1);
        emisor.setUrlRecepcion(SriClienteSoapService.URL_RECEPCION_PRUEBAS);
        emisor.setUrlAutorizacion(SriClienteSoapService.URL_AUTORIZACION_PRUEBAS);
        return emisor;
    }

    @Transactional(readOnly = true)
    public EmisorSriForm cargarFormularioEmisor() {
        EmisorSri e = obtenerEmisorActivo();
        EmisorSriForm f = new EmisorSriForm();
        f.setRuc(e.getRuc());
        f.setRazonSocial(e.getRazonSocial());
        f.setNombreComercial(e.getNombreComercial());
        f.setDirMatriz(e.getDirMatriz());
        f.setDirEstablecimiento(e.getDirEstablecimiento());
        f.setCodEstablecimiento(e.getCodEstablecimiento());
        f.setPuntoEmision(e.getPuntoEmision());
        f.setObligadoContabilidad(e.isObligadoContabilidad());
        f.setContribuyenteEspecial(e.getContribuyenteEspecial());
        f.setAmbiente(e.getAmbiente());
        f.setUrlRecepcion(e.getUrlRecepcion());
        f.setUrlAutorizacion(e.getUrlAutorizacion());
        f.setRutaCertificado(e.getRutaCertificado());
        return f;
    }

    @Transactional
    public EmisorSri guardarEmisor(EmisorSriForm form) {
        EmisorSri e = obtenerEmisorActivo();
        e.setRuc(form.getRuc());
        e.setRazonSocial(form.getRazonSocial());
        e.setNombreComercial(form.getNombreComercial());
        e.setDirMatriz(form.getDirMatriz());
        e.setDirEstablecimiento(form.getDirEstablecimiento());
        e.setCodEstablecimiento(form.getCodEstablecimiento());
        e.setPuntoEmision(form.getPuntoEmision());
        e.setObligadoContabilidad(form.isObligadoContabilidad());
        e.setContribuyenteEspecial(form.getContribuyenteEspecial());
        e.setAmbiente((short) 1);
        e.setTipoEmision((short) 1);
        e.setUrlRecepcion(SriClienteSoapService.URL_RECEPCION_PRUEBAS);
        e.setUrlAutorizacion(SriClienteSoapService.URL_AUTORIZACION_PRUEBAS);
        e.setRutaCertificado(form.getRutaCertificado());
        if (form.getClaveCertificado() != null && !form.getClaveCertificado().isEmpty()) {
            e.setClaveCertificado(form.getClaveCertificado());
        }
        return emisores.save(e);
    }

    @Transactional(readOnly = true)
    public Optional<FacturaSri> buscarEstadoSri(Long idFactura) {
        return facturasSri.findByIdFactura(idFactura);
    }

    /**
     * Consulta la factura autorizando tanto al administrador/empleado emisor
     * como al cliente dueño de la factura (consulta como cliente).
     */
    @Transactional(readOnly = true)
    public FacturaVista consultarComoAdminOCliente(Long idFactura) {
        var cuenta = sesion.actual(false);
        if (cuenta.administrador()) {
            return facturaService.consultar(idFactura);
        }
        Factura f = facturaRepository.completo(idFactura)
                .orElseThrow(() -> new NoSuchElementException("Factura no encontrada."));
        // Verificar si es el empleado emisor O si es el cliente dueño
        boolean esEmpleadoEmisor = false;
        try {
            esEmpleadoEmisor = Objects.equals(sesion.empleado(cuenta.id(), false).getIdEmpleado(), f.getEmpleado().getIdEmpleado());
        } catch (FacturaException ignored) {
            // Cuenta sin empleado (por ejemplo, cuenta de cliente)
        }
        boolean esClienteDueno = personaAsociadaACuenta(cuenta.id())
                .map(idPersona -> Objects.equals(idPersona, f.getIdPersona()))
                .orElse(false);

        if (!esEmpleadoEmisor && !esClienteDueno) {
            throw new AccessDeniedException("No tienes permiso para consultar esta factura.");
        }
        return construirVistaDesdeBd(f);
    }

    /** Lista las facturas donde la cuenta autenticada figura como cliente (por cédula o correo en BD). */
    @Transactional(readOnly = true)
    public List<FacturaVista> listarFacturasComoCliente() {
        var cuenta = sesion.actual(false);
        Optional<Long> idPersona = personaAsociadaACuenta(cuenta.id());
        if (idPersona.isEmpty()) {
            return List.of();
        }
        return facturaRepository.deCliente(idPersona.get()).stream()
                .map(this::construirVistaDesdeBd)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Persona> personaClienteActual() {
        var cuenta = sesion.actual(false);
        return personaAsociadaACuenta(cuenta.id()).flatMap(personaRepository::findById);
    }

    private Optional<Long> personaAsociadaACuenta(Long idUsuario) {
        List<Long> ids = jdbc.query(
                """
                SELECT p.ID_PERSONA
                FROM dbo.Usuario u
                JOIN dbo.Persona p ON (
                    TRIM(p.CEDULA_PERSONA) = TRIM(u.NOMBRE_USUARIO)
                    OR (u.CORREO_USUARIO IS NOT NULL AND p.CORREO_PERSONA IS NOT NULL
                        AND LOWER(TRIM(p.CORREO_PERSONA)) = LOWER(TRIM(u.CORREO_USUARIO)))
                )
                WHERE u.ID_USUARIO = ?
                ORDER BY p.ID_PERSONA ASC
                """,
                (rs, rowNum) -> rs.getLong(1),
                idUsuario);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.getFirst());
    }

    private FacturaVista construirVistaDesdeBd(Factura f) {
        var lineas = detalleRepository.deFactura(f.getIdFactura()).stream()
                .map(d -> new FacturaVista.Linea(
                        d.getProducto().getIdProducto(),
                        d.getProducto().getNombreProducto().stripTrailing(),
                        d.getCantidad(),
                        d.getPrecio(),
                        d.getIva(),
                        d.getSubtotal(),
                        d.getTotal().subtract(d.getSubtotal()),
                        d.getTotal()))
                .toList();
        FacturaCliente hist = historicos.findById(f.getIdFactura()).orElse(null);
        FacturaPago pago = pagos.findByIdFactura(f.getIdFactura()).orElse(null);
        Persona cli = hist == null ? personaRepository.findById(f.getIdPersona()).orElse(null) : null;
        String cedula = hist != null ? hist.getIdentificacion() : (cli == null ? "—" : cli.getCedulaPersona().stripTrailing());
        String nombreCli = hist != null
                ? hist.getNombres() + " " + hist.getApellidos()
                : (cli == null ? "Cliente no disponible" : cli.getNombresPersona().stripTrailing() + " " + cli.getApellidosPersona().stripTrailing());
        String empleadoNombre = f.getEmpleado().getPersona().getNombresPersona().stripTrailing()
                + " " + f.getEmpleado().getPersona().getApellidosPersona().stripTrailing();
        var contacto = hist == null
                ? new FacturaVista.Contacto(null, null, null, false)
                : new FacturaVista.Contacto(hist.getDireccion(), hist.getTelefono(), hist.getCorreo(), true);
        var pagoVista = pago == null ? null : new FacturaVista.Pago(
                pago.getNombre(), pago.getImporte(), pago.getMontoRecibido(), pago.getCambio(),
                pago.getBanco(), pago.getReferencia(), pago.getMarcaTarjeta(), pago.getUltimosDigitos(), pago.getCuotas());
        return new FacturaVista(
                f.getIdFactura(), f.getFecha(), cedula, nombreCli, empleadoNombre,
                f.getSubtotal(), f.getTotal().subtract(f.getSubtotal()), f.getTotal(),
                lineas, contacto, pagoVista);
    }

    /** Genera (o recupera) el comprobante XML SRI consultando BD administrativa y de cliente. */
    @Transactional
    public FacturaSri generarOActualizarXml(Long idFactura) {
        FacturaVista vista = consultarComoAdminOCliente(idFactura);
        EmisorSri emisor = obtenerEmisorActivo();

        String secuencial = SriClaveAcceso.secuencialDesdeId(vista.id());
        String claveAcceso = SriClaveAcceso.generar(
                vista.fecha().toLocalDate(),
                "01",
                emisor.getRuc(),
                emisor.getAmbiente(),
                emisor.getCodEstablecimiento(),
                emisor.getPuntoEmision(),
                secuencial,
                SriClaveAcceso.codigoNumericoDesdeId(vista.id()),
                emisor.getTipoEmision());

        String xmlBase = xmlService.generarXmlFactura(emisor, vista, claveAcceso, secuencial);
        SriFirmaXmlService.FirmaResultado firma = firmaService.firmarSiExiste(xmlBase, emisor);

        FacturaSri sri = facturasSri.findByIdFactura(idFactura).orElseGet(FacturaSri::new);
        sri.setIdFactura(idFactura);
        sri.setAmbiente(emisor.getAmbiente());
        sri.setEstablecimiento(emisor.getCodEstablecimiento());
        sri.setPuntoEmision(emisor.getPuntoEmision());
        sri.setSecuencial(secuencial);
        sri.setClaveAcceso(claveAcceso);
        sri.setXmlComprobante(firma.xml());
        if (sri.getEstadoSri() == null || sri.getEstadoSri().isBlank()) {
            sri.setEstadoSri("GENERADO");
            sri.setExitosoPruebas(false);
            sri.setMensajeRetorno("XML generado desde BD en " + emisor.ambienteEtiqueta() + ". " + firma.detalle() + " Listo para envío al SRI.");
        }
        return facturasSri.save(sri);
    }

    /** Genera el PDF RIDE oficial del SRI consultando encabezado, detalle, emisor y cliente desde BD. */
    @Transactional
    public byte[] generarRidePdf(Long idFactura) {
        FacturaVista vista = consultarComoAdminOCliente(idFactura);
        EmisorSri emisor = obtenerEmisorActivo();
        FacturaSri sri = generarOActualizarXml(idFactura);
        return rideService.generarRide(emisor, vista, sri);
    }

    /** Realiza el envío real al Web Service del SRI en ambiente de pruebas y guarda el retorno explícito. */
    @Transactional
    public EnvioResultado enviarAlSriAmbientePruebas(Long idFactura) {
        if (!ambientePruebas) {
            throw new org.springframework.security.access.AccessDeniedException("El envío SRI requiere la instancia de pruebas.");
        }
        FacturaVista vista = consultarComoAdminOCliente(idFactura);
        FacturaSri sri = generarOActualizarXml(vista.id());

        SriClienteSoapService.RespuestaSri resp = soapService.enviarYConsultarAutorizacion(
                sri.getXmlComprobante(),
                sri.getClaveAcceso(),
                SriClienteSoapService.URL_RECEPCION_PRUEBAS,
                SriClienteSoapService.URL_AUTORIZACION_PRUEBAS,
                (short) 1);

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Guayaquil"));
        sri.setFechaEnvio(ahora);
        sri.setEstadoSri(resp.estado());
        sri.setExitosoPruebas(resp.exitosaEnPruebas());
        sri.setNumeroAutorizacion(resp.numeroAutorizacion());
        if ("AUTORIZADO".equalsIgnoreCase(resp.estado())) {
            sri.setFechaAutorizacion(ahora);
        }
        sri.setMensajeRetorno(resp.mensajeRetorno());
        sri.setXmlRespuestaSri(resp.xmlRespuesta());
        facturasSri.save(sri);

        return new EnvioResultado(
                idFactura,
                sri.getClaveAcceso(),
                resp.estado(),
                resp.exitosaEnPruebas(),
                resp.ambiente(),
                resp.numeroAutorizacion(),
                resp.mensajeRetorno());
    }
}
