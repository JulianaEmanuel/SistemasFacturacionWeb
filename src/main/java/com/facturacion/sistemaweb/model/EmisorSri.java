package com.facturacion.sistemaweb.model;

import jakarta.persistence.*;

@Entity
@Table(name = "EmisorSri", schema = "dbo")
public class EmisorSri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EMISOR", nullable = false)
    private Integer idEmisor;

    @Column(name = "RUC", nullable = false, length = 13)
    private String ruc;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 300)
    private String razonSocial;

    @Column(name = "NOMBRE_COMERCIAL", length = 300)
    private String nombreComercial;

    @Column(name = "DIR_MATRIZ", nullable = false, length = 300)
    private String dirMatriz;

    @Column(name = "DIR_ESTABLECIMIENTO", nullable = false, length = 300)
    private String dirEstablecimiento;

    @Column(name = "COD_ESTABLECIMIENTO", nullable = false, length = 3)
    private String codEstablecimiento = "001";

    @Column(name = "PUNTO_EMISION", nullable = false, length = 3)
    private String puntoEmision = "001";

    @Column(name = "OBLIGADO_CONTABILIDAD", nullable = false)
    private boolean obligadoContabilidad;

    @Column(name = "CONTRIBUYENTE_ESPECIAL", length = 20)
    private String contribuyenteEspecial;

    @Column(name = "AMBIENTE", nullable = false)
    private Short ambiente = 1;

    @Column(name = "TIPO_EMISION", nullable = false)
    private Short tipoEmision = 1;

    @Column(name = "URL_RECEPCION", nullable = false, length = 300)
    private String urlRecepcion;

    @Column(name = "URL_AUTORIZACION", nullable = false, length = 300)
    private String urlAutorizacion;

    @Column(name = "RUTA_CERTIFICADO", length = 500)
    private String rutaCertificado;

    @Column(name = "CLAVE_CERTIFICADO", length = 200)
    private String claveCertificado;

    @Column(name = "ACTIVO", nullable = false)
    private boolean activo = true;

    public Integer getIdEmisor() { return idEmisor; }
    public void setIdEmisor(Integer idEmisor) { this.idEmisor = idEmisor; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getDirMatriz() { return dirMatriz; }
    public void setDirMatriz(String dirMatriz) { this.dirMatriz = dirMatriz; }
    public String getDirEstablecimiento() { return dirEstablecimiento; }
    public void setDirEstablecimiento(String dirEstablecimiento) { this.dirEstablecimiento = dirEstablecimiento; }
    public String getCodEstablecimiento() { return codEstablecimiento; }
    public void setCodEstablecimiento(String codEstablecimiento) { this.codEstablecimiento = codEstablecimiento; }
    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }
    public boolean isObligadoContabilidad() { return obligadoContabilidad; }
    public void setObligadoContabilidad(boolean obligadoContabilidad) { this.obligadoContabilidad = obligadoContabilidad; }
    public String getContribuyenteEspecial() { return contribuyenteEspecial; }
    public void setContribuyenteEspecial(String contribuyenteEspecial) { this.contribuyenteEspecial = contribuyenteEspecial; }
    public Short getAmbiente() { return ambiente; }
    public void setAmbiente(Short ambiente) { this.ambiente = ambiente; }
    public Short getTipoEmision() { return tipoEmision; }
    public void setTipoEmision(Short tipoEmision) { this.tipoEmision = tipoEmision; }
    public String getUrlRecepcion() { return urlRecepcion; }
    public void setUrlRecepcion(String urlRecepcion) { this.urlRecepcion = urlRecepcion; }
    public String getUrlAutorizacion() { return urlAutorizacion; }
    public void setUrlAutorizacion(String urlAutorizacion) { this.urlAutorizacion = urlAutorizacion; }
    public String getRutaCertificado() { return rutaCertificado; }
    public void setRutaCertificado(String rutaCertificado) { this.rutaCertificado = rutaCertificado; }
    public String getClaveCertificado() { return claveCertificado; }
    public void setClaveCertificado(String claveCertificado) { this.claveCertificado = claveCertificado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String ambienteEtiqueta() {
        return ambiente != null && ambiente == 2 ? "PRODUCCIÓN" : "PRUEBAS";
    }
}
