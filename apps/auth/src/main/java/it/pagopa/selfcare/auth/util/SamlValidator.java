package it.pagopa.selfcare.auth.util;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.*;
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

  public boolean validateSamlResponse(String samlResponse, String idpCert, long interval) throws Exception {

    byte[] saml = Base64.getDecoder().decode(samlResponse.getBytes(StandardCharsets.UTF_8));
    String samlResponseXML = new String(saml, StandardCharsets.UTF_8);

    String cleanedXml = cleanXmlContent(samlResponseXML);
    Document doc = parseXmlDocument(cleanedXml);

    if (!isTimestampValid(doc, interval)) {
      throw new SamlSignatureException("Response timestamp is too old. Possible replay attack.");
    }

    X509Certificate certificate = extractCertificateFromSaml(doc, fromBase64(idpCert));
    validateCertificate(certificate);
    return validateSignature(doc, certificate.getPublicKey());
  }

  /**
   * Cleans the XML content from BOM, whitespace, and decodes Base64 if necessary
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
        xml = xml.trim(); // Trim again after decoding
      } catch (IllegalArgumentException e) {
        log.warn("Content is not valid Base64, using original content");
      }
    }

    xml = xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

    return xml;
  }

  public Document parseXmlDocument(String xml) throws Exception {
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
      throw new SecurityException("No X.509 certificate found in the SAML response");
    }

    Node certNode = certNodes.item(0);
    String certContent = certNode.getTextContent().trim();

    certContent = certContent.replaceAll("\\s", "");

    if (!certContent.equals(idpCert)) {
      throw new SecurityException("Incorrect certificate");
    }

    byte[] certBytes = Base64.getDecoder().decode(certContent);

    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
    return (X509Certificate) factory.generateCertificate(bis);
  }

  public void validateCertificate(X509Certificate certificate) throws Exception {
    try {
      certificate.checkValidity();
      log.info("Certificate is valid - valid until: {}", certificate.getNotAfter());
    } catch (CertificateExpiredException e) {
      throw new SecurityException("Certificate expired on: " + certificate.getNotAfter(), e);
    } catch (CertificateNotYetValidException e) {
      throw new SecurityException("Certificate not yet valid until: " + certificate.getNotBefore(), e);
    }
  }

  boolean validateSignature(Document doc, PublicKey publicKey) throws Exception {
    NodeList assertionNodeList = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");
    if (assertionNodeList.getLength() == 0) {
      throw new SamlSignatureException("No <saml2:Assertion> element found in the document.");
    }
    Element assertionElement = (Element) assertionNodeList.item(0);
    assertionElement.setIdAttribute("ID", true);

    NodeList signatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");

    if (signatures.getLength() == 0) {
      throw new SamlSignatureException("No digital signature found in the SAML document");
    }

    Element signatureElement = (Element) signatures.item(0);

    if (!org.apache.xml.security.Init.isInitialized()) {
      org.apache.xml.security.Init.init();
    }
    XMLSignature signature = new XMLSignature(signatureElement, "");
    boolean isValid = signature.checkSignatureValue(publicKey);

    if (!isValid) {
      throw new SamlSignatureException("Digital signature is not valid");
    }

    log.info("Digital signature validated successfully");
    return true;
  }

  public Uni<Boolean> validateSamlResponseAsync(String samlXml, String idpCert, long interval) {
    return Uni.createFrom().item(() -> {
        try {
          return validateSamlResponse(samlXml, idpCert, interval);
        } catch (Exception e) {
          throw new ForbiddenException(e.getMessage());
        }
      })
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
      .onItem().invoke(result -> {
        log.info("SAML validation completed with result: {}", result);
      })
      .onFailure().invoke(throwable -> {
        log.error("Error during SAML validation asincrona", throwable);
      });
  }

  /**
   * Extracts useful information from the SAML response
   */
  public Map<String, Object> extractSamlInfo(Document doc) {
    Map<String, Object> info = new HashMap<>();

    try {
//      String cleanedXml = cleanXmlContent(samlXml);
//      Document doc = parseXmlDocument(cleanedXml);

      NodeList nameIdNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
      if (nameIdNodes.getLength() > 0) {
        info.put("name_id", nameIdNodes.item(0).getTextContent());
      }

      // Extract attributes
      NodeList attributeStatements = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AttributeStatement");
      for (int i = 0; i < attributeStatements.getLength(); i++) {
        Node attributeStatement = attributeStatements.item(i);
        NodeList attributeStatementChildren = attributeStatement.getChildNodes();
        for (int j = 0; j < attributeStatementChildren.getLength(); j++) {
          Node attribute = attributeStatementChildren.item(j);
          NamedNodeMap attributes = attribute.getAttributes();
          String attributeName = "";
          for (int k = 0; k < attributes.getLength(); k++) {
            Node attributeOnAttribute = attributes.item(k);
            if (attributeOnAttribute.getLocalName().equals("Name")) {
              attributeName = attributeOnAttribute.getTextContent();
            }
          }
          NodeList attributeValues = attribute.getChildNodes();
          StringBuilder attributeValueSerialized = new StringBuilder();
          for (int k = 0; k < attributeValues.getLength(); k++) {
            Element attributeValue = (Element) attributeValues.item(k);
            if (k > 0) {
              attributeValueSerialized.append(",");
            }
            attributeValueSerialized.append(attributeValue.getTextContent());
          }
          info.put(attributeName, attributeValueSerialized.toString());
        }
      }

      // Extract Issuer
      NodeList issuerNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
      if (issuerNodes.getLength() > 0) {
        info.put("issuer", issuerNodes.item(0).getTextContent());
      }

      // Extract SessionIndex
      NodeList authnNodes = doc.getElementsByTagNameNS(
        "urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");
      if (authnNodes.getLength() > 0) {
        Element authnElement = (Element) authnNodes.item(0);
        String sessionIndex = authnElement.getAttribute("SessionIndex");
        if (!sessionIndex.isEmpty()) {
          info.put("session_index", sessionIndex);
        }
      }

    } catch (Exception e) {
      log.error("Error during SAML information extraction", e);
      info.put("error", e.getMessage());
    }

    return info;
  }

  public String fromBase64(String publicCert) {
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
   * Verifies that the IssueInstant attribute of the SAML response is not older than a certain interval.
   *
   * @param doc SAML XML response.
   * @param maxSeconds The maximum allowed difference in seconds.
   * @return true if timestamp is valid, false otherwise.
   */
  public boolean isTimestampValid(Document doc, long maxSeconds) {

    NodeList responseNodeList = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:protocol", "Response");

    if (responseNodeList.getLength() == 0) {
      log.error("No <saml2p:Response> element found in the document.");
      return false;
    }
    Element responseElement = (Element) responseNodeList.item(0);

    String issueInstantString = responseElement.getAttribute("IssueInstant");
    if (issueInstantString.isBlank()) {
      log.error("'IssueInstant' attribute not found or empty in the SAML response.");
      return false;
    }

    Instant issueInstant = Instant.parse(issueInstantString);
    Instant now = Instant.now();

    Duration timeDifference = Duration.between(issueInstant, now);

    log.info("Response timestamp: {}. Current time: {}. Difference: {} seconds.",
      issueInstant, now, timeDifference.toSeconds());

    return Math.abs(timeDifference.toSeconds()) <= maxSeconds;
  }
}
