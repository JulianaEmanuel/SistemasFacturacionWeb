package com.facturacion.sistemaweb.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;

@ControllerAdvice
public class CuentaActualAdvice {
    private final org.springframework.beans.factory.ObjectProvider<com.facturacion.sistemaweb.security.AdminAccess> admin;
    public CuentaActualAdvice(org.springframework.beans.factory.ObjectProvider<com.facturacion.sistemaweb.security.AdminAccess> admin) {this.admin=admin;}
    @Value("#{environment.matchesProfiles('pruebas')}")
    private boolean ambientePruebas;

    @Value("${app.ambiente.url-pruebas:}")
    private String urlPruebas;

    @ModelAttribute("ambientePruebas")
    public boolean ambientePruebas() {
        return ambientePruebas;
    }

    @ModelAttribute("urlPruebas")
    public String urlPruebas() { return urlPruebas; }

    @Value("${APP_PASE_PRUEBAS_CLAVE:}")
    private String clavePase;

    @Value("${APP_PASE_PRUEBAS_ORIGEN:}")
    private String origenPase;

    @Value("${app.ambiente.url-principal:}")
    private String urlPrincipal;

    @ModelAttribute("urlPrincipal")
    public String urlPrincipal() { return urlPrincipal; }

    @ModelAttribute("accesoDirectoPruebas")
    public boolean accesoDirectoPruebas(Principal principal) {
        return !ambientePruebas && principal != null && !clavePase.isBlank()
            && principal.getName().equals(origenPase) && administracion();
    }

    @ModelAttribute("cuentaActual") public String cuenta(Principal principal) { return principal==null?"":principal.getName(); }
    @ModelAttribute("administracionDisponible") public boolean administracion() {
        var acceso=admin.getIfAvailable();
        return acceso!=null && acceso.allowed(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    }
}
