package com.facturacion.sistemaweb.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulario administrativo para configurar los datos del emisor SRI y ambiente de pruebas. */
public class EmisorSriForm {

    @NotBlank(message = "El RUC del emisor es obligatorio.")
    @Pattern(regexp = "[0-9]{13}", message = "El RUC debe tener exactamente 13 dígitos numéricos.")
    private String ruc;

    @NotBlank(message = "La razón social es obligatoria.")
    @Size(max = 300, message = "La razón social admite hasta 300 caracteres.")
    private String razonSocial;

    @Size(max = 300, message = "El nombre comercial admite hasta 300 caracteres.")
    private String nombreComercial;

    @NotBlank(message = "La dirección matriz es obligatoria.")
    @Size(max = 300, message = "La dirección matriz admite hasta 300 caracteres.")
    private String dirMatriz;

    @NotBlank(message = "La dirección del establecimiento es obligatoria.")
    @Size(max = 300, message = "La dirección del establecimiento admite hasta 300 caracteres.")
    private String dirEstablecimiento;

    @NotBlank(message = "El código de establecimiento es obligatorio.")
    @Pattern(regexp = "[0-9]{3}", message = "El establecimiento debe tener 3 dígitos (ej. 001).")
    private String codEstablecimiento = "001";

    @NotBlank(message = "El punto de emisión es obligatorio.")
    @Pattern(regexp = "[0-9]{3}", message = "El punto de emisión debe tener 3 dígitos (ej. 001).")
    private String puntoEmision = "001";

    private boolean obligadoContabilidad;

    @Size(max = 20, message = "El código de contribuyente especial admite hasta 20 caracteres.")
    private String contribuyenteEspecial;

    private Short ambiente = 1;

    @NotBlank(message = "La URL de recepción del SRI es obligatoria.")
    @Size(max = 300)
    private String urlRecepcion = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";

    @NotBlank(message = "La URL de autorización del SRI es obligatoria.")
    @Size(max = 300)
    private String urlAutorizacion = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";

    @Size(max = 500, message = "La ruta del certificado admite hasta 500 caracteres.")
    private String rutaCertificado;

    @Size(max = 200, message = "La clave del certificado admite hasta 200 caracteres.")
    private String claveCertificado;

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc == null ? null : ruc.strip(); }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial == null ? null : razonSocial.strip(); }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial == null ? null : nombreComercial.strip(); }
    public String getDirMatriz() { return dirMatriz; }
    public void setDirMatriz(String dirMatriz) { this.dirMatriz = dirMatriz == null ? null : dirMatriz.strip(); }
    public String getDirEstablecimiento() { return dirEstablecimiento; }
    public void setDirEstablecimiento(String dirEstablecimiento) { this.dirEstablecimiento = dirEstablecimiento == null ? null : dirEstablecimiento.strip(); }
    public String getCodEstablecimiento() { return codEstablecimiento; }
    public void setCodEstablecimiento(String codEstablecimiento) { this.codEstablecimiento = codEstablecimiento == null ? null : codEstablecimiento.strip(); }
    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision == null ? null : puntoEmision.strip(); }
    public boolean isObligadoContabilidad() { return obligadoContabilidad; }
    public void setObligadoContabilidad(boolean obligadoContabilidad) { this.obligadoContabilidad = obligadoContabilidad; }
    public String getContribuyenteEspecial() { return contribuyenteEspecial; }
    public void setContribuyenteEspecial(String contribuyenteEspecial) { this.contribuyenteEspecial = contribuyenteEspecial == null ? null : contribuyenteEspecial.strip(); }
    public Short getAmbiente() { return ambiente; }
    public void setAmbiente(Short ambiente) { this.ambiente = ambiente; }
    public String getUrlRecepcion() { return urlRecepcion; }
    public void setUrlRecepcion(String urlRecepcion) { this.urlRecepcion = urlRecepcion == null ? null : urlRecepcion.strip(); }
    public String getUrlAutorizacion() { return urlAutorizacion; }
    public void setUrlAutorizacion(String urlAutorizacion) { this.urlAutorizacion = urlAutorizacion == null ? null : urlAutorizacion.strip(); }
    public String getRutaCertificado() { return rutaCertificado; }
    public void setRutaCertificado(String rutaCertificado) { this.rutaCertificado = rutaCertificado == null ? null : rutaCertificado.strip(); }
    public String getClaveCertificado() { return claveCertificado; }
    public void setClaveCertificado(String claveCertificado) { this.claveCertificado = claveCertificado; }
}
