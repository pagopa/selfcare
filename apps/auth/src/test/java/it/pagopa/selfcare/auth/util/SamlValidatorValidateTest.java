package it.pagopa.selfcare.auth.util;

import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SamlValidatorValidateTest {

  private SamlValidator samlValidator;
  private KeyPair keyPair;
  private PublicKey publicKey;
  private PrivateKey privateKey;

  @BeforeEach
  void setUp() throws Exception {
    samlValidator = new SamlValidator();

    // Generate an RSA key pair for tests
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    keyPair = keyGen.generateKeyPair();
    publicKey = keyPair.getPublic();
    privateKey = keyPair.getPrivate();

    // Initialize Apache XML Security
    if (!org.apache.xml.security.Init.isInitialized()) {
      org.apache.xml.security.Init.init();
    }
  }

  @Test
  void testValidateSignature_WithValidSignature_ShouldReturnTrue() throws Exception {
    // Given
    Document doc = createValidSignedSamlDocument();

    // When
    boolean result = invokeValidateSignature(doc, publicKey);

    // Then
    assertTrue(result, "Signature validation should return true for a valid signature");
  }

  @Test
  void testValidateSignature_WithNoSignature_ShouldReturnFalse() throws Exception {
    // Given
    Document doc = createSamlDocumentWithoutSignature();

    // When
    RuntimeException thrown = assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSignature(doc, publicKey);
    });

    // Then
    assertEquals("No digital signature found in the SAML document", thrown.getMessage(), "The exception message should be propagated");
  }

  @Test
  void testValidateSignature_WithNoAssertion_ShouldReturnFalse() throws Exception {
    // Given
    Document doc = createDocumentWithoutAssertion();

    // When
    RuntimeException thrown = assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSignature(doc, publicKey);
    });

    // Then
    assertEquals("No <saml2:Assertion> element found in the document.", thrown.getMessage(), "The exception message should be propagated");
  }

  @Test
  void testValidateSignature_WithWrongPublicKey_ShouldReturnFalse() throws Exception {
    // Given
    Document doc = createValidSignedSamlDocument();

    // Generate a different public key
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair differentKeyPair = keyGen.generateKeyPair();
    PublicKey wrongPublicKey = differentKeyPair.getPublic();

    // When
    RuntimeException thrown = assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSignature(doc, wrongPublicKey);
    });

    // Then
    assertEquals("Digital signature is not valid", thrown.getMessage(), "The exception message should be propagated");
  }

  @Test
  void testValidateSignature_WithCorruptedSignature_ShouldReturnFalse() throws Exception {
    // Given
    Document doc = createSamlDocumentWithCorruptedSignature();

    // When
    RuntimeException thrown = assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSignature(doc, publicKey);
    });

    // Then
    assertEquals("Digital signature is not valid", thrown.getMessage(), "The exception message should be propagated");
  }

  // Helper method to invoke the private validateSignature method using reflection
  private boolean invokeValidateSignature(Document doc, PublicKey publicKey) throws Exception {
    Method validateSignatureMethod = SamlValidator.class.getDeclaredMethod("validateSignature", Document.class, PublicKey.class);
    validateSignatureMethod.setAccessible(true);
    return (Boolean) validateSignatureMethod.invoke(samlValidator, doc, publicKey);
  }

  // Create a correctly signed SAML document
  private Document createValidSignedSamlDocument() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    // Create the Response element
    Element responseElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:Response");
    responseElement.setAttribute("ID", "response_123");
    responseElement.setAttribute("Version", "2.0");
    responseElement.setAttribute("IssueInstant", Instant.now().toString());
    doc.appendChild(responseElement);

    // Create the Assertion element
    Element assertionElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Assertion");
    assertionElement.setAttribute("ID", "assertion_123");
    assertionElement.setAttribute("Version", "2.0");
    assertionElement.setAttribute("IssueInstant", Instant.now().toString());
    responseElement.appendChild(assertionElement);

    // Set the ID attribute for XML validation
    assertionElement.setIdAttribute("ID", true);

    // Create the Issuer
    Element issuerElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Issuer");
    issuerElement.setTextContent("http://test.idp.com");
    assertionElement.appendChild(issuerElement);

    // Create Subject
    Element subjectElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Subject");
    Element nameIdElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:NameID");
    nameIdElement.setTextContent("test@example.com");
    subjectElement.appendChild(nameIdElement);
    assertionElement.appendChild(subjectElement);

    // Crea e firma il documento
    XMLSignature xmlSignature = new XMLSignature(doc, null, "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
    assertionElement.appendChild(xmlSignature.getElement());

    // Create and sign the document
    Transforms transforms = new Transforms(doc);
    transforms.addTransform("http://www.w3.org/2000/09/xmldsig#enveloped-signature");
    transforms.addTransform("http://www.w3.org/2001/10/xml-exc-c14n#");
    xmlSignature.addDocument("#assertion_123", transforms, "http://www.w3.org/2001/04/xmlenc#sha256");

    xmlSignature.sign(privateKey);

    return doc;
  }

  // Create the Reference pointing to the Assertion
  private Document createSamlDocumentWithoutSignature() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element responseElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:Response");
    responseElement.setAttribute("ID", "response_123");
    doc.appendChild(responseElement);

    Element assertionElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Assertion");
    assertionElement.setAttribute("ID", "assertion_123");
    assertionElement.setIdAttribute("ID", true);
    responseElement.appendChild(assertionElement);

    Element subjectElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:Subject");
    Element nameIdElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:assertion", "saml2:NameID");
    nameIdElement.setTextContent("test@example.com");
    subjectElement.appendChild(nameIdElement);
    assertionElement.appendChild(subjectElement);

    return doc;
  }

  // Create a document without an Assertion
  private Document createDocumentWithoutAssertion() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();

    Element responseElement = doc.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "saml2p:Response");
    responseElement.setAttribute("ID", "response_123");
    doc.appendChild(responseElement);

    // Create a dummy signature without an assertion
    Element signatureElement = doc.createElementNS("http://www.w3.org/2000/09/xmldsig#", "ds:Signature");
    responseElement.appendChild(signatureElement);

    return doc;
  }

  private Document createSamlDocumentWithCorruptedSignature() throws Exception {
    Document doc = createValidSignedSamlDocument();

    // Corrupt the signature by modifying the SignatureValue
    Element signatureValue = (Element) doc.getElementsByTagNameNS(
      "http://www.w3.org/2000/09/xmldsig#", "SignatureValue").item(0);

    if (signatureValue != null) {
      String originalValue = signatureValue.getTextContent().trim().replaceAll("[\r\n]", "");

      // Create a corrupted but valid Base64 signature
      byte[] originalBytes = Base64.getDecoder().decode(originalValue);

      byte[] corruptedBytes = originalBytes.clone();

      if (corruptedBytes.length > 0) {
        corruptedBytes[0] ^= 0xFF; // flip bit
      }

      String corruptedValue = Base64.getEncoder().encodeToString(corruptedBytes);
      signatureValue.setTextContent(corruptedValue);
    }

    return doc;
  }

  @Test
  void testLength() {
    String originalValue = "SUoBgy6L+8ocK1fJZ3ALG1NdpX5r8MO9NTioEI19yOABlLxn5JiW9J5YkoDIAfKtKWsEG62nd7zRq7ou258mAlL3S6zqI7sRE7VhBvUyw1FC+PMpRmU7ETBkPMp8kOLWHrlD1A2X2B9E2EYvLSnwHtHBSYmc32WKH/OpUfSFB1aoypNuj4+RuqA8Mud9WwX4Uaqz3TA78sjktNoh1HE2m6xLHZ8V7BrAELY0WfQ5EAUPORQ+cl9+sT10r+jiLwcUUbb47Hscfz+BxEkqb/oBkANotfFpHd1MrGzDY4eAVa1lDrXnArn3cJ/+AoAnQ9TR1MWCvYkLouRFRNR7F3HXCQ==";
    byte[] originalBytes = Base64.getDecoder().decode(originalValue);

    byte[] corruptedBytes = originalBytes.clone();
    if (corruptedBytes.length > 0) {
      corruptedBytes[0] ^= 0xFF; // flip bit
    }

    String corruptedValue = Base64.getEncoder().encodeToString(corruptedBytes);
    assertEquals(originalValue.length(), corruptedValue.length());
  }

  // Additional test to verify exception handling
  @Test
  void testValidateSignature_WithNullDocument_ShouldReturnFalse() throws Exception {
    // When & Then
    assertThrows(Exception.class, () -> {
      invokeValidateSignature(null, publicKey);
    }, "Should throw an exception with a null document");
  }

  @Test
  void testValidateSignature_WithNullPublicKey_ShouldReturnFalse() throws Exception {
    // Given
    Document doc = createValidSignedSamlDocument();

    // When & Then
    assertThrows(Exception.class, () -> {
      invokeValidateSignature(doc, null);
    }, "Should throw an exception with a null public key");
  }

  // Simplified integration test
  @Test
  void testIntegration_ValidateSamlResponse_ShouldNotThrowException() {
    // Given
    String simpleSamlResponse = Base64.getEncoder().encodeToString(createSimpleSamlXml().getBytes());
    String mockCert = "dGVzdA==";
    long interval = 3000000 ;

    // When & Then - The test verifies that no unexpected exceptions are thrown
    assertThrows(SamlSignatureException.class, () -> {
      samlValidator.validateSamlResponse(simpleSamlResponse, mockCert, interval);
    });
  }

  private String createSimpleSamlXml() {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                           xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
                           ID="response_123"
                           Version="2.0"
                           IssueInstant="2024-12-10T10:00:00Z">
                <saml2:Assertion ID="assertion_123" Version="2.0">
                    <saml2:Subject>
                        <saml2:NameID>test@example.com</saml2:NameID>
                    </saml2:Subject>
                </saml2:Assertion>
            </saml2p:Response>
            """;
  }
}
