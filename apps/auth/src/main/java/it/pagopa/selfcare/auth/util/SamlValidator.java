package it.pagopa.selfcare.auth.util;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ApplicationScoped
public class SamlValidator {

  public boolean validateSamlResponse(String samlResponse, String idpCert, long interval) {
    try {
      byte[] saml = Base64.getDecoder().decode(samlResponse.getBytes(StandardCharsets.UTF_8));
      String samlResponseXML = new String(saml, StandardCharsets.UTF_8);

      String cleanedXml = cleanXmlContent(samlResponseXML);
      Document doc = parseXmlDocument(cleanedXml);
      boolean isRecentEnough = isTimestampValid(doc, interval);
      if (!isRecentEnough) {
        System.out.println("Il timestamp della risposta è troppo vecchio. Possibile attacco di tipo replay.");
        return false;
      }
      X509Certificate certificate = extractCertificateFromSaml(doc, fromBase64(idpCert));

      validateCertificate(certificate);

      return validateSignature(doc, certificate.getPublicKey());

    } catch (Exception e) {
      log.error("Errore nella validazione SAML", e);
      return false;
    }
  }

  /**
   * Pulisce il contenuto XML da BOM, spazi bianchi e decodifica Base64 se necessario
   */
  public String cleanXmlContent(String xml) {
    if (xml == null || xml.isEmpty()) {
      throw new IllegalArgumentException("XML content is null or empty");
    }
    if (xml.startsWith("\uFEFF")) {
      xml = xml.substring(1);
    }

    xml = xml.trim();

    if (!xml.startsWith("<?xml") && !xml.startsWith("<")) {
      try {
        byte[] decoded = Base64.getDecoder().decode(xml);
        xml = new String(decoded, StandardCharsets.UTF_8);
        xml = xml.trim(); // Trim ancora dopo la decodifica
      } catch (IllegalArgumentException e) {
        log.warn("Content non è Base64 valido, uso contenuto originale");
      }
    }

    xml = xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

    return xml;
  }

  private Document parseXmlDocument(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

    DocumentBuilder builder = factory.newDocumentBuilder();

    ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    InputSource inputSource = new InputSource(bis);
    inputSource.setEncoding("UTF-8");

    return builder.parse(inputSource);
  }

  private X509Certificate extractCertificateFromSaml(Document doc, String idpCert) throws Exception {

    NodeList certNodes = doc.getElementsByTagNameNS(
      "http://www.w3.org/2000/09/xmldsig#", "X509Certificate");

    if (certNodes.getLength() == 0) {
      throw new SecurityException("Nessun certificato X.509 trovato nella risposta SAML");
    }

    Node certNode = certNodes.item(0);
    String certContent = certNode.getTextContent().trim();

    certContent = certContent.replaceAll("\\s", "");

    if (!certContent.equals(idpCert)) {
      throw new SecurityException("Certificato non corretto");
    }

    byte[] certBytes = Base64.getDecoder().decode(certContent);

    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
    return (X509Certificate) factory.generateCertificate(bis);
  }

  private void validateCertificate(X509Certificate certificate) throws Exception {
    try {
      certificate.checkValidity();
      log.info("Certificato valido - valido fino al: {}", certificate.getNotAfter());
    } catch (CertificateExpiredException e) {
      throw new SecurityException("Certificato scaduto il: " + certificate.getNotAfter(), e);
    } catch (CertificateNotYetValidException e) {
      throw new SecurityException("Certificato non ancora valido fino al: " + certificate.getNotBefore(), e);
    }
  }

  private boolean validateSignature(Document doc, PublicKey publicKey) throws Exception {
    NodeList assertionNodeList = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");
    if (assertionNodeList.getLength() == 0) {
      log.error("Nessun elemento <saml2:Assertion> trovato nel documento.");
      return false;
    }
    Element assertionElement = (Element) assertionNodeList.item(0);
    assertionElement.setIdAttribute("ID", true);

    NodeList signatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");

    if (signatures.getLength() == 0) {
      log.error("Nessuna firma digitale trovata nel documento SAML");
      return false;
    }

    Element signatureElement = (Element) signatures.item(0);

    if (!org.apache.xml.security.Init.isInitialized()) {
      org.apache.xml.security.Init.init();
    }
    XMLSignature signature = new XMLSignature(signatureElement, "");
    boolean isValid = signature.checkSignatureValue(publicKey);

    if (isValid) {
      log.info("Firma digitale validata con successo");
    } else {
      log.error("Firma digitale non valida");
    }

    return isValid;
  }

  public Uni<Boolean> validateSamlResponseAsync(String samlXml, String idpCert, long interval) {
    return Uni.createFrom().item(() -> validateSamlResponse(samlXml, idpCert, interval))
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
      .onItem().invoke(result -> {
        log.info("Validazione SAML completata con risultato: {}", result);
      })
      .onFailure().invoke(throwable -> {
        log.error("Errore nella validazione SAML asincrona", throwable);
      });
  }

  /**
   * Estrae informazioni utili dalla risposta SAML
   */
  public Map<String, Object> extractSamlInfo(String samlXml) {
    Map<String, Object> info = new HashMap<>();

    try {
      String cleanedXml = cleanXmlContent(samlXml);
      Document doc = parseXmlDocument(cleanedXml);

      NodeList nameIdNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
      if (nameIdNodes.getLength() > 0) {
        info.put("name_id", nameIdNodes.item(0).getTextContent());
      }

      // Estrai Issuer
      NodeList issuerNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
      if (issuerNodes.getLength() > 0) {
        info.put("issuer", issuerNodes.item(0).getTextContent());
      }

      // Estrai SessionIndex
      NodeList authnNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");
      if (authnNodes.getLength() > 0) {
        Element authnElement = (Element) authnNodes.item(0);
        String sessionIndex = authnElement.getAttribute("SessionIndex");
        if (!sessionIndex.isEmpty()) {
          info.put("session_index", sessionIndex);
        }
      }

//      // Informazioni sul certificato
//      try {
//        X509Certificate cert = extractCertificateFromSaml(doc, "");
//        info.put("certificate_subject", cert.getSubjectDN().toString());
//        info.put("certificate_issuer", cert.getIssuerDN().toString());
//        info.put("certificate_valid_until", cert.getNotAfter());
//      } catch (Exception e) {
//        log.warn("Impossibile estrarre informazioni certificato", e);
//      }

    } catch (Exception e) {
      log.error("Errore nell'estrazione informazioni SAML", e);
      info.put("error", e.getMessage());
    }

    return info;
  }

  private String fromBase64(String publicCert) throws Exception {
    byte[] keyBytes = Base64.getDecoder().decode(publicCert);
    String publicCertDecoded = new String(keyBytes, StandardCharsets.UTF_8);

    log.info("idpCert {}", publicCertDecoded);
    String publicCertContent = publicCertDecoded
      .replace("-----BEGIN CERTIFICATE-----", "")
      .replace("-----END CERTIFICATE-----", "")
      .replaceAll("\\s", "");
    log.info("idpCert {}", publicCertContent);
    return publicCertContent;
  }

  /**
   * Verifica che l'attributo IssueInstant della risposta SAML non sia più vecchio di un certo intervallo.
   *
   * @param doc La risposta SAML come stringa XML.
   * @param maxSeconds La massima differenza in secondi consentita.
   * @return true se il timestamp è valido, false altrimenti.
   */
  public boolean isTimestampValid(Document doc, long maxSeconds) {

    NodeList responseNodeList = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:protocol", "Response");

    if (responseNodeList.getLength() == 0) {
      log.error("Nessun elemento <saml2p:Response> trovato nel documento.");
      return false;
    }
    Element responseElement = (Element) responseNodeList.item(0);

    String issueInstantString = responseElement.getAttribute("IssueInstant");
    if (issueInstantString.isBlank()) {
      log.error("Attributo 'IssueInstant' non trovato o vuoto nella risposta SAML.");
      return false;
    }

    Instant issueInstant = Instant.parse(issueInstantString);
    Instant now = Instant.now();

    Duration timeDifference = Duration.between(issueInstant, now);

    log.info("Timestamp della risposta: {}. Ora corrente: {}. Differenza: {} secondi.",
      issueInstant, now, timeDifference.toSeconds());

    return Math.abs(timeDifference.toSeconds()) <= maxSeconds;
  }
}
