package it.pagopa.selfcare.auth.util;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.WorkerExecutor;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@ApplicationScoped
public class SamlValidator {

  public boolean validateSamlResponse(String samlResponse, String idpCert, long interval) throws Exception {
    return createValidationPipeline(interval, fromBase64(idpCert)).apply(samlResponse);
  }

  public Uni<Boolean> validateSamlResponseAsync(String samlResponse, String idpCert, long interval) {
    return createAsyncValidationPipeline(interval, fromBase64(idpCert))
      .apply(samlResponse)
      .onItem().invoke(result -> log.info("SAML validation completed with result: {}", result));
  }

  private Function<String, Boolean> createValidationPipeline(long interval, String idpCertContent) {
    return samlResponse -> Optional.of(samlResponse)
      .map(this::decodeSamlResponse)
      .map(this::cleanXmlContent)
      .map(this::parseXmlDocumentSafe)
      .filter(doc -> isTimestampValid(doc, interval))
      .map(doc -> createCertificateDocument(doc, idpCertContent))
      .filter(this::validateCertificateSafe)
      .map(this::validateSignatureSafe)
      .orElseThrow(() -> new SamlSignatureException("SAML validation failed"));
  }

  private Function<String, Uni<Boolean>> createAsyncValidationPipeline(long interval, String idpCertContent) {
    return samlResponse -> Uni.createFrom().item(samlResponse)
      .onItem().transform(this::decodeSamlResponse)
      .onItem().transform(this::cleanXmlContent)
      .onItem().transformToUni(this::parseXmlDocumentAsync)
      .onItem().transformToUni(doc -> validateTimestampAsync(doc, interval))
      .onItem().transformToUni(doc -> extractCertificateAsync(doc, idpCertContent))
      .onItem().transformToUni(this::validateCertificateAsync)
      .onItem().transformToUni(this::validateSignatureAsync)
      .onFailure().transform(this::mapToSamlException);
  }

  // ==================== OPERATIONS ====================

  private String decodeSamlResponse(String samlResponse) {
    return Optional.ofNullable(samlResponse)
      .map(response -> response.getBytes(StandardCharsets.UTF_8))
      .map(Base64.getDecoder()::decode)
      .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
      .orElseThrow(() -> new IllegalArgumentException("Invalid SAML response"));
  }

  public String cleanXmlContent(String xml) {
    return Optional.ofNullable(xml)
      .filter(Predicate.not(String::isEmpty))
      .map(this::removeBom)
      .map(String::trim)
      .map(this::decodeBase64IfNeeded)
      .map(this::removeControlCharacters)
      .orElseThrow(() -> new IllegalArgumentException("XML content is null or empty"));
  }

  private String removeBom(String xml) {
    return xml.startsWith("\uFEFF") ? xml.substring(1) : xml;
  }

  private String decodeBase64IfNeeded(String xml) {
    return (!xml.startsWith("<?xml") && !xml.startsWith("<"))
      ? tryDecodeBase64(xml)
      : xml;
  }

  private String tryDecodeBase64(String xml) {
    try {
      byte[] decoded = Base64.getDecoder().decode(xml);
      return new String(decoded, StandardCharsets.UTF_8).trim();
    } catch (IllegalArgumentException e) {
      log.warn("Content is not valid Base64, using original content");
      return xml;
    }
  }

  private String removeControlCharacters(String xml) {
    return xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
  }

  public Document parseXmlDocument(String xml) throws Exception {
    DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
    DocumentBuilder builder = factory.newDocumentBuilder();

    ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    InputSource inputSource = new InputSource(bis);
    inputSource.setEncoding("UTF-8");

    return builder.parse(inputSource);
  }

  private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory;
  }

  private Document parseXmlDocumentSafe(String xml) {
    try {
      return parseXmlDocument(xml);
    } catch (Exception e) {
      throw new SamlSignatureException("Failed to parse XML document");
    }
  }

  private Uni<Document> parseXmlDocumentAsync(String xml) {
    return Uni.createFrom().item(() -> parseXmlDocumentSafe(xml))
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }

  public boolean isTimestampValid(Document doc, long maxSeconds) {
    return extractTimestamp(doc)
      .map(this::parseInstant)
      .map(issueInstant -> validateTimestampDifference(issueInstant, maxSeconds))
      .orElse(false);
  }

  private Optional<String> extractTimestamp(Document doc) {
    NodeList responseNodeList = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:protocol", "Response");

    return (responseNodeList.getLength() > 0)
      ? Optional.of(((Element) responseNodeList.item(0)).getAttribute("IssueInstant"))
      .filter(Predicate.not(String::isBlank))
      : Optional.empty();
  }

  private Instant parseInstant(String issueInstantString) {
    try {
      return Instant.parse(issueInstantString);
    } catch (Exception e) {
      log.error("Failed to parse timestamp: {}", issueInstantString);
      throw new SamlSignatureException("Invalid timestamp format");
    }
  }

  private boolean validateTimestampDifference(Instant issueInstant, long maxSeconds) {
    Instant now = Instant.now();
    Duration timeDifference = Duration.between(issueInstant, now);

    log.info("Response timestamp: {}. Current time: {}. Difference: {} seconds.",
      issueInstant, now, timeDifference.toSeconds());

    return Math.abs(timeDifference.toSeconds()) <= maxSeconds;
  }

  private Uni<Document> validateTimestampAsync(Document doc, long interval) {
    return isTimestampValid(doc, interval)
      ? Uni.createFrom().item(doc)
      : Uni.createFrom().failure(new SamlSignatureException("Response timestamp is too old. Possible replay attack."));
  }

  private CertificateDocument createCertificateDocument(Document doc, String idpCertContent) {
    try {
      X509Certificate certificate = extractCertificateFromSaml(doc, idpCertContent);
      return new CertificateDocument(certificate, doc);
    } catch (Exception e) {
      throw new SamlSignatureException("Failed to extract certificate");
    }
  }

  private X509Certificate extractCertificateFromSaml(Document doc, String idpCert) throws Exception {
    String certContent = extractCertificateContent(doc);
    validateCertificateContent(certContent, idpCert);
    return createCertificateFromContent(certContent);
  }

  private String extractCertificateContent(Document doc) {
    NodeList certNodes = doc.getElementsByTagNameNS(
      "http://www.w3.org/2000/09/xmldsig#", "X509Certificate");

    return Optional.of(certNodes)
      .filter(nodes -> nodes.getLength() > 0)
      .map(nodes -> nodes.item(0).getTextContent().trim())
      .map(content -> content.replaceAll("\\s", ""))
      .orElseThrow(() -> new SecurityException("No X.509 certificate found in the SAML response"));
  }

  private void validateCertificateContent(String certContent, String idpCert) {
    Optional.of(certContent)
      .filter(content -> content.equals(idpCert))
      .orElseThrow(() -> new SecurityException("Incorrect certificate"));
  }

  private X509Certificate createCertificateFromContent(String certContent) throws Exception {
    byte[] certBytes = Base64.getDecoder().decode(certContent);
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
    return (X509Certificate) factory.generateCertificate(bis);
  }

  private Uni<CertificateDocument> extractCertificateAsync(Document doc, String idpCertContent) {
    return Uni.createFrom().item(() -> createCertificateDocument(doc, idpCertContent))
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
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

  private boolean validateCertificateSafe(CertificateDocument certDoc) {
    try {
      validateCertificate(certDoc.certificate());
      return true;
    } catch (Exception e) {
      throw new SamlSignatureException("Certificate validation failed");
    }
  }

  private Uni<CertificateDocument> validateCertificateAsync(CertificateDocument certDoc) {
    return Uni.createFrom().item(() -> {
        try {
          validateCertificate(certDoc.certificate());
        } catch (Exception e) {
          throw new SamlSignatureException("Certificate validation failed");
        }
        return certDoc;
      })
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }

  /**
   * Validazione firma digitale
   */
  boolean validateSignature(Document doc, PublicKey publicKey) throws Exception {
    extractAndSetupAssertion(doc);
    Element signatureElement = extractSignatureElement(doc);

    return validateXmlSignature(signatureElement, publicKey);
  }

  private Element extractAndSetupAssertion(Document doc) {
    NodeList assertionNodeList = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");

    return Optional.of(assertionNodeList)
      .filter(nodes -> nodes.getLength() > 0)
      .map(nodes -> (Element) nodes.item(0))
      .map(element -> {
        element.setIdAttribute("ID", true);
        return element;
      })
      .orElseThrow(() -> new SamlSignatureException("No <saml2:Assertion> element found in the document."));
  }

  private Element extractSignatureElement(Document doc) {
    NodeList signatures = doc.getElementsByTagNameNS(
      "http://www.w3.org/2000/09/xmldsig#", "Signature");

    return Optional.of(signatures)
      .filter(nodes -> nodes.getLength() > 0)
      .map(nodes -> (Element) nodes.item(0))
      .orElseThrow(() -> new SamlSignatureException("No digital signature found in the SAML document"));
  }

  private boolean validateXmlSignature(Element signatureElement, PublicKey publicKey) throws Exception {
    initializeXmlSecurity();

    XMLSignature signature = new XMLSignature(signatureElement, "");
    boolean isValid = signature.checkSignatureValue(publicKey);

    return Optional.of(isValid)
      .filter(Boolean::booleanValue)
      .map(valid -> {
        log.info("Digital signature validated successfully");
        return true;
      })
      .orElseThrow(() -> new SamlSignatureException("Digital signature is not valid"));
  }

  private void initializeXmlSecurity() {
    Optional.of(org.apache.xml.security.Init.isInitialized())
      .filter(initialized -> !initialized)
      .ifPresent(unused -> org.apache.xml.security.Init.init());
  }

  private boolean validateSignatureSafe(CertificateDocument certDoc) {
    try {
      return validateSignature(certDoc.document(), certDoc.certificate().getPublicKey());
    } catch (Exception e) {
      throw new SamlSignatureException("Signature validation failed");
    }
  }

  private Uni<Boolean> validateSignatureAsync(CertificateDocument certDoc) {
    return Uni.createFrom().item(() -> validateSignatureSafe(certDoc))
      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
  }

  /**
   * Estrazione informazioni SAML
   */
  public Map<String, Object> extractSamlInfo(Document doc) {
    return createSamlInfoExtractor().apply(doc);
  }

  private Function<Document, Map<String, Object>> createSamlInfoExtractor() {
    return doc -> {
      Map<String, Object> info = new HashMap<>();

      extractNameId(doc).ifPresent(nameId -> info.put("name_id", nameId));
      info.putAll(extractAttributes(doc));
      extractIssuer(doc).ifPresent(issuer -> info.put("issuer", issuer));
      extractSessionIndex(doc).ifPresent(sessionIndex -> info.put("session_index", sessionIndex));

      return info;
    };
  }

  private Optional<String> extractNameId(Document doc) {
    return extractTextContent(doc, "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
  }

  private Optional<String> extractIssuer(Document doc) {
    return extractTextContent(doc, "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
  }

  private Optional<String> extractSessionIndex(Document doc) {
    NodeList authnNodes = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");

    return (authnNodes.getLength() > 0)
      ? Optional.of(((Element) authnNodes.item(0)).getAttribute("SessionIndex"))
      .filter(Predicate.not(String::isEmpty))
      : Optional.empty();
  }

  private Optional<String> extractTextContent(Document doc, String namespaceUri, String localName) {
    NodeList nodes = doc.getElementsByTagNameNS(namespaceUri, localName);
    return (nodes.getLength() > 0)
      ? Optional.of(nodes.item(0).getTextContent())
      : Optional.empty();
  }

  private Map<String, String> extractAttributes(Document doc) {
    NodeList attributeStatements = doc.getElementsByTagNameNS(
      "urn:oasis:names:tc:SAML:2.0:assertion", "AttributeStatement");

    Map<String, String> attributes = new HashMap<>();

    for (int i = 0; i < attributeStatements.getLength(); i++) {
      processAttributeStatement(attributeStatements.item(i), attributes);
    }

    return attributes;
  }

  private void processAttributeStatement(Node attributeStatement, Map<String, String> attributes) {
    NodeList children = attributeStatement.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      Node attribute = children.item(i);
      if (attribute.getNodeType() == Node.ELEMENT_NODE) {
        extractAttributeNameAndValue(attribute)
          .ifPresent(entry -> attributes.put(entry.getKey(), entry.getValue()));
      }
    }
  }

  private Optional<Map.Entry<String, String>> extractAttributeNameAndValue(Node attribute) {
    return extractAttributeName(attribute)
      .flatMap(name -> extractAttributeValue(attribute)
        .map(value -> Map.entry(name, value)));
  }

  private Optional<String> extractAttributeName(Node attribute) {
    return Optional.ofNullable(attribute.getAttributes())
      .map(attrs -> attrs.getNamedItem("Name"))
      .map(Node::getTextContent);
  }

  private Optional<String> extractAttributeValue(Node attribute) {
    NodeList valueNodes = attribute.getChildNodes();
    StringBuilder value = new StringBuilder();

    for (int i = 0; i < valueNodes.getLength(); i++) {
      if (valueNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
        if (value.length() > 0) value.append(",");
        value.append(valueNodes.item(i).getTextContent());
      }
    }

    return value.length() > 0 ? Optional.of(value.toString()) : Optional.empty();
  }

  /**
   * Estrazione certificato da Base64
   */
  public String fromBase64(String publicCert) {
    return Optional.ofNullable(publicCert)
      .map(Base64.getDecoder()::decode)
      .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
      .map(this::extractCertificateContent)
      .orElseThrow(() -> new IllegalArgumentException("Invalid certificate"));
  }

  private String extractCertificateContent(String cert) {
    return cert.replace("-----BEGIN CERTIFICATE-----", "")
      .replace("-----END CERTIFICATE-----", "")
      .replaceAll("\\s", "");
  }

  // ==================== TYPED SAFE METHODS ====================



  // ==================== UTILITY METHODS ====================

  private RuntimeException mapToSamlException(Throwable throwable) {
    if (throwable instanceof SamlSignatureException) {
      return (SamlSignatureException) throwable;
    } else if (throwable instanceof RuntimeException) {
      return (RuntimeException) throwable;
    } else {
      return new SamlSignatureException("SAML validation failed");
    }
  }

  // ==================== RECORDS & SEALED CLASSES ====================

  public record CertificateDocument(X509Certificate certificate, Document document) {}

//
//  public boolean validateSamlResponseOLD(String samlResponse, String idpCert, long interval) throws Exception {
//
//    byte[] saml = Base64.getDecoder().decode(samlResponse.getBytes(StandardCharsets.UTF_8));
//    String samlResponseXML = new String(saml, StandardCharsets.UTF_8);
//
//    String cleanedXml = cleanXmlContent(samlResponseXML);
//    Document doc = parseXmlDocument(cleanedXml);
//
//    if (!isTimestampValid(doc, interval)) {
//      throw new SamlSignatureException("Response timestamp is too old. Possible replay attack.");
//    }
//
//    X509Certificate certificate = extractCertificateFromSaml(doc, fromBase64(idpCert).toString());
//    validateCertificate(certificate);
//    return validateSignature(doc, certificate.getPublicKey());
//  }
//
//  /**
//   * Cleans the XML content from BOM, whitespace, and decodes Base64 if necessary
//   */
//  public String cleanXmlContent(String xml) {
//    if (xml == null || xml.isEmpty()) {
//      throw new IllegalArgumentException("XML content is null or empty");
//    }
//    if (xml.startsWith("\uFEFF")) {
//      xml = xml.substring(1);
//    }
//
//    xml = xml.trim();
//
//    if (!xml.startsWith("<?xml") && !xml.startsWith("<")) {
//      try {
//        byte[] decoded = Base64.getDecoder().decode(xml);
//        xml = new String(decoded, StandardCharsets.UTF_8);
//        xml = xml.trim(); // Trim again after decoding
//      } catch (IllegalArgumentException e) {
//        log.warn("Content is not valid Base64, using original content");
//      }
//    }
//
//    xml = xml.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
//
//    return xml;
//  }
//
//  public Document parseXmlDocument(String xml) throws Exception {
//    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//    factory.setNamespaceAware(true);
//    factory.setValidating(false);
//    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
//
//    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//
//    DocumentBuilder builder = factory.newDocumentBuilder();
//
//    ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
//    InputSource inputSource = new InputSource(bis);
//    inputSource.setEncoding("UTF-8");
//
//    return builder.parse(inputSource);
//  }
//
//  private X509Certificate extractCertificateFromSaml(Document doc, String idpCert) throws Exception {
//
//    NodeList certNodes = doc.getElementsByTagNameNS(
//      "http://www.w3.org/2000/09/xmldsig#", "X509Certificate");
//
//    if (certNodes.getLength() == 0) {
//      throw new SecurityException("No X.509 certificate found in the SAML response");
//    }
//
//    Node certNode = certNodes.item(0);
//    String certContent = certNode.getTextContent().trim();
//
//    certContent = certContent.replaceAll("\\s", "");
//
//    if (!certContent.equals(idpCert)) {
//      throw new SecurityException("Incorrect certificate");
//    }
//
//    byte[] certBytes = Base64.getDecoder().decode(certContent);
//
//    CertificateFactory factory = CertificateFactory.getInstance("X.509");
//    ByteArrayInputStream bis = new ByteArrayInputStream(certBytes);
//    return (X509Certificate) factory.generateCertificate(bis);
//  }
//
//  public void validateCertificate(X509Certificate certificate) throws Exception {
//    try {
//      certificate.checkValidity();
//      log.info("Certificate is valid - valid until: {}", certificate.getNotAfter());
//    } catch (CertificateExpiredException e) {
//      throw new SecurityException("Certificate expired on: " + certificate.getNotAfter(), e);
//    } catch (CertificateNotYetValidException e) {
//      throw new SecurityException("Certificate not yet valid until: " + certificate.getNotBefore(), e);
//    }
//  }
//
//  boolean validateSignature(Document doc, PublicKey publicKey) throws Exception {
//    NodeList assertionNodeList = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion");
//    if (assertionNodeList.getLength() == 0) {
//      throw new SamlSignatureException("No <saml2:Assertion> element found in the document.");
//    }
//    Element assertionElement = (Element) assertionNodeList.item(0);
//    assertionElement.setIdAttribute("ID", true);
//
//    NodeList signatures = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
//
//    if (signatures.getLength() == 0) {
//      throw new SamlSignatureException("No digital signature found in the SAML document");
//    }
//
//    Element signatureElement = (Element) signatures.item(0);
//
//    if (!org.apache.xml.security.Init.isInitialized()) {
//      org.apache.xml.security.Init.init();
//    }
//    XMLSignature signature = new XMLSignature(signatureElement, "");
//    boolean isValid = signature.checkSignatureValue(publicKey);
//
//    if (!isValid) {
//      throw new SamlSignatureException("Digital signature is not valid");
//    }
//
//    log.info("Digital signature validated successfully");
//    return true;
//  }
//
//  public Uni<Boolean> validateSamlResponseAsync(String samlXml, String idpCert, long interval) throws Exception {
//    return Uni.createFrom().item(() -> {
//        try {
//          return validateSamlResponse(samlXml, idpCert, interval);
//        } catch (Exception e) {
//          throw new SamlSignatureException(e.getMessage());
//        }
//      })
//      .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
//      .onItem().invoke(result -> {
//        log.info("SAML validation completed with result: {}", result);
//      });
//  }
//
//  /**
//   * Extracts useful information from the SAML response
//   */
//  public Map<String, Object> extractSamlInfo(Document doc) {
//    Map<String, Object> info = new HashMap<>();
//
//    try {
////      String cleanedXml = cleanXmlContent(samlXml);
////      Document doc = parseXmlDocument(cleanedXml);
//
//      NodeList nameIdNodes = doc.getElementsByTagNameNS(
//        "urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
//      if (nameIdNodes.getLength() > 0) {
//        info.put("name_id", nameIdNodes.item(0).getTextContent());
//      }
//
//      // Extract attributes
//      NodeList attributeStatements = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "AttributeStatement");
//      for (int i = 0; i < attributeStatements.getLength(); i++) {
//        Node attributeStatement = attributeStatements.item(i);
//        NodeList attributeStatementChildren = attributeStatement.getChildNodes();
//        for (int j = 0; j < attributeStatementChildren.getLength(); j++) {
//          Node attribute = attributeStatementChildren.item(j);
//          NamedNodeMap attributes = attribute.getAttributes();
//          String attributeName = "";
//          for (int k = 0; k < attributes.getLength(); k++) {
//            Node attributeOnAttribute = attributes.item(k);
//            if (attributeOnAttribute.getLocalName().equals("Name")) {
//              attributeName = attributeOnAttribute.getTextContent();
//            }
//          }
//          NodeList attributeValues = attribute.getChildNodes();
//          StringBuilder attributeValueSerialized = new StringBuilder();
//          for (int k = 0; k < attributeValues.getLength(); k++) {
//            Element attributeValue = (Element) attributeValues.item(k);
//            if (k > 0) {
//              attributeValueSerialized.append(",");
//            }
//            attributeValueSerialized.append(attributeValue.getTextContent());
//          }
//          info.put(attributeName, attributeValueSerialized.toString());
//        }
//      }
//
//      // Extract Issuer
//      NodeList issuerNodes = doc.getElementsByTagNameNS(
//        "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
//      if (issuerNodes.getLength() > 0) {
//        info.put("issuer", issuerNodes.item(0).getTextContent());
//      }
//
//      // Extract SessionIndex
//      NodeList authnNodes = doc.getElementsByTagNameNS(
//        "urn:oasis:names:tc:SAML:2.0:assertion", "AuthnStatement");
//      if (authnNodes.getLength() > 0) {
//        Element authnElement = (Element) authnNodes.item(0);
//        String sessionIndex = authnElement.getAttribute("SessionIndex");
//        if (!sessionIndex.isEmpty()) {
//          info.put("session_index", sessionIndex);
//        }
//      }
//
//    } catch (Exception e) {
//      log.error("Error during SAML information extraction", e);
//      info.put("error", e.getMessage());
//    }
//
//    return info;
//  }
//
//  public byte[] fromBase64(String publicCert) {
//    byte[] keyBytes = Base64.getDecoder().decode(publicCert);
//    String publicCertDecoded = new String(keyBytes, StandardCharsets.UTF_8);
//
//    log.info("idpCert {}", publicCertDecoded);
//    String publicCertContent = publicCertDecoded
//      .replace("-----BEGIN CERTIFICATE-----", "")
//      .replace("-----END CERTIFICATE-----", "")
//      .replaceAll("\\s", "");
//    log.info("idpCert {}", publicCertContent);
//    return publicCertContent.getBytes(StandardCharsets.UTF_8);
//  }
//
//  /**
//   * Verifies that the IssueInstant attribute of the SAML response is not older than a certain interval.
//   *
//   * @param doc SAML XML response.
//   * @param maxSeconds The maximum allowed difference in seconds.
//   * @return true if timestamp is valid, false otherwise.
//   */
//  public boolean isTimestampValid(Document doc, long maxSeconds) {
//
//    NodeList responseNodeList = doc.getElementsByTagNameNS(
//      "urn:oasis:names:tc:SAML:2.0:protocol", "Response");
//
//    if (responseNodeList.getLength() == 0) {
//      log.error("No <saml2p:Response> element found in the document.");
//      return false;
//    }
//    Element responseElement = (Element) responseNodeList.item(0);
//
//    String issueInstantString = responseElement.getAttribute("IssueInstant");
//    if (issueInstantString.isBlank()) {
//      log.error("'IssueInstant' attribute not found or empty in the SAML response.");
//      return false;
//    }
//
//    Instant issueInstant = Instant.parse(issueInstantString);
//    Instant now = Instant.now();
//
//    Duration timeDifference = Duration.between(issueInstant, now);
//
//    log.info("Response timestamp: {}. Current time: {}. Difference: {} seconds.",
//      issueInstant, now, timeDifference.toSeconds());
//
//    return Math.abs(timeDifference.toSeconds()) <= maxSeconds;
//  }
}
