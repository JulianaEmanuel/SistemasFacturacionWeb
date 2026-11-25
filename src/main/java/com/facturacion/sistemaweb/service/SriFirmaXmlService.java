package com.facturacion.sistemaweb.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.facturacion.sistemaweb.model.EmisorSri;

/**
 * Firma XML en formato XAdES-BES (enveloped) cuando se dispone de un certificado PKCS#12 (.p12/.pfx).
 * Si no hay certificado configurado, conserva el XML estándar para envío al ambiente de pruebas SRI.
 */
@Service
public class SriFirmaXmlService {

    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String XMLDSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final DateTimeFormatter ISO_EC = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public record FirmaResultado(String xml, boolean firmado, String detalle) {}

    private final String envCertPath;
    private final String envCertPassword;

    public SriFirmaXmlService(
            @Value("${app.sri.cert-path:}") String envCertPath,
            @Value("${app.sri.cert-password:}") String envCertPassword) {
        this.envCertPath = envCertPath == null ? "" : envCertPath.strip();
        this.envCertPassword = envCertPassword == null ? "" : envCertPassword;
    }

    public FirmaResultado firmarSiExiste(String xmlOriginal, EmisorSri emisor) {
        String ruta = emisor != null && emisor.getRutaCertificado() != null && !emisor.getRutaCertificado().isBlank()
                ? emisor.getRutaCertificado().strip()
                : envCertPath;
        String clave = emisor != null && emisor.getClaveCertificado() != null && !emisor.getClaveCertificado().isEmpty()
                ? emisor.getClaveCertificado()
                : envCertPassword;

        if (ruta.isEmpty()) {
            return new FirmaResultado(xmlOriginal, false, "Sin archivo .p12 configurado; se envía XML sin firma XAdES-BES.");
        }
        Path path = Path.of(ruta);
        if (!Files.isRegularFile(path)) {
            return new FirmaResultado(xmlOriginal, false, "No se encontró el archivo .p12 en la ruta configurada.");
        }
        try {
            String xmlFirmado = firmarXadesBes(xmlOriginal, path, clave.toCharArray());
            return new FirmaResultado(xmlFirmado, true, "XML firmado con XAdES-BES usando el certificado configurado.");
        } catch (Exception ex) {
            return new FirmaResultado(xmlOriginal, false, "No se pudo firmar con el certificado .p12 (" + ex.getClass().getSimpleName() + ").");
        }
    }

    public String firmarXadesBes(String xml, Path p12Path, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(p12Path)) {
            ks.load(in, password);
        }
        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String candidate = aliases.nextElement();
            if (ks.isKeyEntry(candidate)) {
                alias = candidate;
                break;
            }
        }
        if (alias == null) {
            throw new KeyStoreException("El almacén PKCS#12 no contiene clave privada.");
        }
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        String sigId = "Signature-" + UUID.randomUUID().toString().substring(0, 8);
        String signedPropsId = "SignedProperties-" + UUID.randomUUID().toString().substring(0, 8);
        String certId = "Certificate-" + UUID.randomUUID().toString().substring(0, 8);
        String refId = "Reference-Comprobante-" + UUID.randomUUID().toString().substring(0, 8);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        DigestMethod sha256 = fac.newDigestMethod(DigestMethod.SHA256, null);

        Reference refComprobante = fac.newReference(
                "#comprobante",
                sha256,
                List.of(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                refId);
        Reference refSignedProps = fac.newReference(
                "#" + signedPropsId,
                sha256,
                null,
                "http://uri.etsi.org/01903#SignedProperties",
                null);
        Reference refCert = fac.newReference("#" + certId, sha256);

        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                List.of(refComprobante, refSignedProps, refCert));

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(List.of(cert));
        KeyInfo ki = kif.newKeyInfo(List.of(x509Data), certId);

        Element qualProps = construirQualifyingProperties(doc, sigId, signedPropsId, refId, cert);
        XMLObject xadesObj = fac.newXMLObject(List.of(new DOMStructure(qualProps)), null, null, null);

        XMLSignature signature = fac.newXMLSignature(si, ki, List.of(xadesObj), sigId, null);
        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());
        dsc.setIdAttributeNS(doc.getDocumentElement(), null, "id");
        dsc.putNamespacePrefix(XMLDSIG_NS, "ds");
        signature.sign(dsc);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        trans.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    private static Element construirQualifyingProperties(
            Document doc, String sigId, String signedPropsId, String refId, X509Certificate cert) throws Exception {
        Element qp = doc.createElementNS(XADES_NS, "etsi:QualifyingProperties");
        qp.setAttribute("Target", "#" + sigId);

        Element sp = doc.createElementNS(XADES_NS, "etsi:SignedProperties");
        sp.setAttribute("Id", signedPropsId);
        sp.setIdAttribute("Id", true);

        Element ssp = doc.createElementNS(XADES_NS, "etsi:SignedSignatureProperties");
        Element signingTime = doc.createElementNS(XADES_NS, "etsi:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now(ZoneId.of("America/Guayaquil")).format(ISO_EC));
        ssp.appendChild(signingTime);

        Element sc = doc.createElementNS(XADES_NS, "etsi:SigningCertificate");
        Element certElem = doc.createElementNS(XADES_NS, "etsi:Cert");
        Element certDigest = doc.createElementNS(XADES_NS, "etsi:CertDigest");
        Element digestMethod = doc.createElementNS(XMLDSIG_NS, "ds:DigestMethod");
        digestMethod.setAttribute("Algorithm", DigestMethod.SHA256);
        Element digestValue = doc.createElementNS(XMLDSIG_NS, "ds:DigestValue");
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        digestValue.setTextContent(Base64.getEncoder().encodeToString(hash));
        certDigest.appendChild(digestMethod);
        certDigest.appendChild(digestValue);

        Element issuerSerial = doc.createElementNS(XADES_NS, "etsi:IssuerSerial");
        Element issuerName = doc.createElementNS(XMLDSIG_NS, "ds:X509IssuerName");
        issuerName.setTextContent(cert.getIssuerX500Principal().getName());
        Element serialNumber = doc.createElementNS(XMLDSIG_NS, "ds:X509SerialNumber");
        serialNumber.setTextContent(cert.getSerialNumber().toString());
        issuerSerial.appendChild(issuerName);
        issuerSerial.appendChild(serialNumber);

        certElem.appendChild(certDigest);
        certElem.appendChild(issuerSerial);
        sc.appendChild(certElem);
        ssp.appendChild(sc);

        Element sdop = doc.createElementNS(XADES_NS, "etsi:SignedDataObjectProperties");
        Element dof = doc.createElementNS(XADES_NS, "etsi:DataObjectFormat");
        dof.setAttribute("ObjectReference", "#" + refId);
        Element desc = doc.createElementNS(XADES_NS, "etsi:Description");
        desc.setTextContent("contenido comprobante");
        Element mime = doc.createElementNS(XADES_NS, "etsi:MimeType");
        mime.setTextContent("text/xml");
        dof.appendChild(desc);
        dof.appendChild(mime);
        sdop.appendChild(dof);

        sp.appendChild(ssp);
        sp.appendChild(sdop);
        qp.appendChild(sp);
        return qp;
    }
}
