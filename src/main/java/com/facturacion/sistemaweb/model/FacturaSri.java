package com.facturacion.sistemaweb.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "FacturaSri", schema = "dbo")
public class FacturaSri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_FACTURA_SRI", nullable = false)
    private Long idFacturaSri;

    @Column(name = "ID_FACTURA", nullable = false, unique = true)
    private Long idFactura;

    @Column(name = "AMBIENTE", nullable = false)
    private Short ambiente;

    @Column(name = "ESTABLECIMIENTO", nullable = false, length = 3)
    private String establecimiento;

    @Column(name = "PUNTO_EMISION", nullable = false, length = 3)
    private String puntoEmision;

    @Column(name = "SECUENCIAL", nullable = false, length = 9)
    private String secuencial;

    @Column(name = "CLAVE_ACCESO", nullable = false, length = 49)
    private String claveAcceso;

    @Column(name = "NUMERO_AUTORIZACION", length = 49)
    private String numeroAutorizacion;

    @Column(name = "FECHA_AUTORIZACION")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "ESTADO_SRI", nullable = false, length = 30)
    private String estadoSri;

    @Column(name = "EXITOSO_PRUEBAS", nullable = false)
    private boolean exitosoPruebas;

    @Column(name = "MENSAJE_RETORNO", columnDefinition = "TEXT")
    private String mensajeRetorno;

    @Column(name = "XML_COMPROBANTE", nullable = false, columnDefinition = "TEXT")
    private String xmlComprobante;

    @Column(name = "XML_RESPUESTA_SRI", columnDefinition = "TEXT")
    private String xmlRespuestaSri;

    @Column(name = "FECHA_ENVIO")
    private LocalDateTime fechaEnvio;

    public Long getIdFacturaSri() { return idFacturaSri; }
    public Long getIdFactura() { return idFactura; }
    public void setIdFactura(Long idFactura) { this.idFactura = idFactura; }
    public Short getAmbiente() { return ambiente; }
    public void setAmbiente(Short ambiente) { this.ambiente = ambiente; }
    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }
    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }
    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String secuencial) { this.secuencial = secuencial; }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public String getNumeroAutorizacion() { return numeroAutorizacion; }
    public void setNumeroAutorizacion(String numeroAutorizacion) { this.numeroAutorizacion = numeroAutorizacion; }
    public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
    public String getEstadoSri() { return estadoSri; }
    public void setEstadoSri(String estadoSri) { this.estadoSri = estadoSri; }
    public boolean isExitosoPruebas() { return exitosoPruebas; }
    public void setExitosoPruebas(boolean exitosoPruebas) { this.exitosoPruebas = exitosoPruebas; }
    public String getMensajeRetorno() { return mensajeRetorno; }
    public void setMensajeRetorno(String mensajeRetorno) { this.mensajeRetorno = mensajeRetorno; }
    public String getXmlComprobante() { return xmlComprobante; }
    public void setXmlComprobante(String xmlComprobante) { this.xmlComprobante = xmlComprobante; }
    public String getXmlRespuestaSri() { return xmlRespuestaSri; }
    public void setXmlRespuestaSri(String xmlRespuestaSri) { this.xmlRespuestaSri = xmlRespuestaSri; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public String numeroComprobante() {
        return establecimiento + "-" + puntoEmision + "-" + secuencial;
    }

    public String ambienteTexto() {
        return ambiente != null && ambiente == 2 ? "PRODUCCIÓN" : "PRUEBAS";
    }
}
