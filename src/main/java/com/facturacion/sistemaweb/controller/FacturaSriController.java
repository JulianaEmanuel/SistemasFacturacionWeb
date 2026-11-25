package com.facturacion.sistemaweb.controller;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.facturacion.sistemaweb.model.*;
import com.facturacion.sistemaweb.service.FacturaSriService;

@Controller
@Profile("sqlserver")
@PreAuthorize("isAuthenticated()")
public class FacturaSriController {

    private final FacturaSriService sriService;

    public FacturaSriController(FacturaSriService sriService) {
        this.sriService = sriService;
    }

    /** Descarga el XML oficial del SRI (formato factura v1.1.0) generado desde la BD. */
    @GetMapping("/modulos/facturas/{idFactura}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long idFactura) {
        FacturaSri sri = sriService.generarOActualizarXml(idFactura);
        byte[] bytes = sri.getXmlComprobante().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + sri.numeroComprobante() + "-sri.xml\"")
                .body(bytes);
    }

    /** Genera y muestra/descarga el RIDE oficial en PDF con código de barras Code 128. */
    @GetMapping("/modulos/facturas/{idFactura}/ride")
    public ResponseEntity<byte[]> descargarRide(@PathVariable Long idFactura) {
        FacturaSri sri = sriService.generarOActualizarXml(idFactura);
        byte[] pdf = sriService.generarRidePdf(idFactura);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"RIDE-factura-" + sri.numeroComprobante() + ".pdf\"")
                .body(pdf);
    }

    /** Envía el comprobante al Web Service SOAP del SRI en Ambiente de Pruebas y retorna el resultado explícito. */
    @PostMapping("/modulos/facturas/{idFactura}/enviar-sri")
    public String enviarAlSri(
            @PathVariable Long idFactura,
            @RequestParam(defaultValue = "admin") String origen,
            RedirectAttributes flash) {
        var resultado = sriService.enviarAlSriAmbientePruebas(idFactura);
        flash.addFlashAttribute(resultado.exitosaEnPruebas() ? "sriSuccess" : "sriWarning", resultado.mensajeRetorno());
        return "cliente".equals(origen)
                ? "redirect:/modulos/mis-facturas/" + idFactura
                : "redirect:/modulos/facturas/" + idFactura;
    }

    /** Endpoint JSON con el retorno explícito bajo ambiente de pruebas (útil para API / verificación). */
    @GetMapping("/modulos/facturas/{idFactura}/sri/estado")
    @ResponseBody
    public FacturaSriService.EnvioResultado consultarEstadoJson(@PathVariable Long idFactura) {
        sriService.consultarComoAdminOCliente(idFactura);
        FacturaSri sri = sriService.generarOActualizarXml(idFactura);
        return new FacturaSriService.EnvioResultado(
                idFactura,
                sri.getClaveAcceso(),
                sri.getEstadoSri(),
                sri.isExitosoPruebas(),
                "AMBIENTE DE " + sri.ambienteTexto(),
                sri.getNumeroAutorizacion(),
                sri.getMensajeRetorno());
    }

    /** Configuración administrativa del Emisor SRI en base de datos (solo Administrador). */
    @GetMapping("/modulos/sri/configuracion")
    public String verConfiguracionSri(Model model) {
        model.addAttribute("emisorForm", sriService.cargarFormularioEmisor());
        return "sri/configuracion";
    }

    @PostMapping("/modulos/sri/configuracion")
    public String guardarConfiguracionSri(
            @Valid @ModelAttribute("emisorForm") EmisorSriForm form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return "sri/configuracion";
        }
        sriService.guardarEmisor(form);
        flash.addFlashAttribute("successMessage", "Configuración administrativa del emisor SRI actualizada en base de datos.");
        return "redirect:/modulos/sri/configuracion";
    }

    /** Vista de base de datos como Cliente: el usuario consulta sus facturas, XML y RIDE. */
    @GetMapping("/modulos/mis-facturas")
    public String misFacturas(Model model) {
        model.addAttribute("clientePersona", sriService.personaClienteActual().orElse(null));
        model.addAttribute("facturas", sriService.listarFacturasComoCliente());
        return "sri/mis-facturas";
    }

    @GetMapping("/modulos/mis-facturas/{idFactura}")
    public String miFacturaDetalle(@PathVariable Long idFactura, Model model) {
        model.addAttribute("factura", sriService.consultarComoAdminOCliente(idFactura));
        model.addAttribute("sri", sriService.buscarEstadoSri(idFactura).orElse(null));
        model.addAttribute("emisor", sriService.obtenerEmisorActivo());
        return "sri/mi-factura-detalle";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String inexistente() {
        return "No se encontró la factura solicitada.";
    }
}
