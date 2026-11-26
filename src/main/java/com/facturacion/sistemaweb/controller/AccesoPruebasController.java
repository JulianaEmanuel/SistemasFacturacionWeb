package com.facturacion.sistemaweb.controller;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import com.facturacion.sistemaweb.security.AdminAccess;
import com.facturacion.sistemaweb.security.AutenticacionUsuarios;
import com.facturacion.sistemaweb.security.PasePruebas;

@Controller
@Profile("postgres")
public class AccesoPruebasController {
    private final PasePruebas pases;
    private final AdminAccess admin;
    private final AutenticacionUsuarios usuarios;
    private final boolean pruebas;
    private final String origen;
    private final String destino;
    private final URI urlPruebas;

    public AccesoPruebasController(PasePruebas pases, AdminAccess admin, AutenticacionUsuarios usuarios,
            @Value("#{environment.matchesProfiles('pruebas')}") boolean pruebas,
            @Value("${APP_PASE_PRUEBAS_ORIGEN:}") String origen,
            @Value("${APP_PASE_PRUEBAS_DESTINO:admin_pruebas}") String destino,
            @Value("${app.ambiente.url-pruebas}") URI urlPruebas) {
        this.pases = pases;
        this.admin = admin;
        this.usuarios = usuarios;
        this.pruebas = pruebas;
        this.origen = origen;
        this.destino = destino;
        this.urlPruebas = urlPruebas;
    }

    @PostMapping("/ambiente/ir-a-pruebas")
    @PreAuthorize("@adminAccess.allowed(authentication)")
    public ResponseEntity<Void> salir(Authentication actual) {
        if (pruebas || !pases.disponible() || !actual.getName().equals(origen)) {
            throw new AccessDeniedException("El acceso directo a pruebas requiere la cuenta configurada.");
        }
        URI puente = urlPruebas.resolve("ambiente/entrar#" + pases.emitir());
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header("Cache-Control", "no-store").location(puente).build();
    }

    @GetMapping("/ambiente/entrar")
    public String puente(HttpServletResponse response) {
        exigirPruebas();
        response.setHeader("Cache-Control", "no-store");
        return "sri/entrar-pruebas";
    }

    @PostMapping("/ambiente/entrar")
    public String entrar(@RequestParam String pase, HttpServletRequest request, HttpServletResponse response) {
        exigirPruebas();
        if (!pases.consumir(pase)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El pase de pruebas venció o ya se usó.");
        }

        UserDetails cuenta;
        try {
            cuenta = usuarios.loadUserByUsername(destino);
        } catch (UsernameNotFoundException ex) {
            throw new AccessDeniedException("La cuenta de pruebas no está disponible.");
        }
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(cuenta.getUsername(), null,
                cuenta.getAuthorities());
        if (!cuenta.isEnabled() || !admin.allowed(autenticacion)) {
            throw new AccessDeniedException("La cuenta de pruebas no está habilitada como administradora.");
        }

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
        return "redirect:/";
    }

    private void exigirPruebas() {
        if (!pruebas || !pases.disponible()) {
            throw new AccessDeniedException("El pase solo se puede usar en el ambiente de pruebas.");
        }
    }
}
