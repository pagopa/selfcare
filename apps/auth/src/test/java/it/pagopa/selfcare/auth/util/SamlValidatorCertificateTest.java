package it.pagopa.selfcare.auth.util;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

@QuarkusTest
class SamlValidatorCertificateTest {

  @Inject SamlValidator samlValidator;

  private Method extractCertificateMethod;
  private X509Certificate testCertificate;
  private String testCertificateBase64;
  /**
   * Test helper that generates real self-signed X.509 certificates at runtime using BouncyCastle.
   *
   * <p>Certificates are created with dynamic validity windows (relative to {@link Instant#now()}),
   * so the tests never break because of a hard-coded expiration date.
   */
  private static class CertificateTestUtils {
    /** Generates a self-signed certificate valid within the given time window. */
    public static X509Certificate generateCertificate(Instant notBefore, Instant notAfter)
        throws Exception {
      KeyPair keyPair = generateKeyPair();
      X500Name dn = new X500Name("CN=test.io, O=TestOrg, C=IT");
      BigInteger serial = BigInteger.valueOf(System.nanoTime());
      JcaX509v3CertificateBuilder certBuilder =
          new JcaX509v3CertificateBuilder(
              dn, serial, Date.from(notBefore), Date.from(notAfter), dn, keyPair.getPublic());
      ContentSigner signer =
          new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
      X509CertificateHolder holder = certBuilder.build(signer);
      return new JcaX509CertificateConverter().getCertificate(holder);
    }
    /** Generates a certificate that is currently valid (yesterday -> tomorrow). */
    public static X509Certificate generateValidCertificate() throws Exception {
      Instant now = Instant.now();
      return generateCertificate(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));
    }
    /** Generates a certificate that is already expired (2 days ago -> 1 day ago). */
    public static X509Certificate generateExpiredCertificate() throws Exception {
      Instant now = Instant.now();
      return generateCertificate(now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS));
    }
    public static KeyPair generateKeyPair() throws Exception {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(2048);
      return kpg.generateKeyPair();
    }
  }

  @Test
  void validateCertificate_ValidCertificate_ShouldNotThrowException() throws Exception {
    X509Certificate validCertificate = CertificateTestUtils.generateValidCertificate();

    // Act & Assert
    assertDoesNotThrow(
        () -> samlValidator.validateCertificate(validCertificate),
        "A valid certificate should not throw an exception.");
  }

  @BeforeEach
  void setUp() throws Exception {
    // Make the private method accessible for testing
    extractCertificateMethod =
        SamlValidator.class.getDeclaredMethod(
            "extractCertificateFromSaml", Document.class, String.class);
    extractCertificateMethod.setAccessible(true);

    // Generate a valid certificate to use in tests
    testCertificate = CertificateTestUtils.generateValidCertificate();
    testCertificateBase64 = Base64.getEncoder().encodeToString(testCertificate.getEncoded());
  }

  private Document createSamlDocumentWithCert(String certificateContent) throws Exception {
    String xml =
        String.format(
            """
      <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol">
          <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
              <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                  <ds:KeyInfo>
                      <ds:X509Data>
                          <ds:X509Certificate>%s</ds:X509Certificate>
                      </ds:X509Data>
                  </ds:KeyInfo>
              </ds:Signature>
          </saml2:Assertion>
      </saml2p:Response>
      """,
            certificateContent);
    // Use the public clean/parse methods from the class itself to get the Document
    String cleanedXml = samlValidator.cleanXmlContent(xml);
    Method parseMethod = SamlValidator.class.getDeclaredMethod("parseXmlDocument", String.class);
    parseMethod.setAccessible(true);
    return (Document) parseMethod.invoke(samlValidator, cleanedXml);
  }

  @Test
  void extractCertificateFromSaml_Success() throws Exception {
    // Arrange
    Document doc = createSamlDocumentWithCert(testCertificateBase64);

    // Act
    X509Certificate extractedCert =
        (X509Certificate)
            extractCertificateMethod.invoke(samlValidator, doc, testCertificateBase64);

    // Assert
    assertNotNull(extractedCert, "The extracted certificate should not be null.");
    assertEquals(
        testCertificate,
        extractedCert,
        "The extracted certificate should be identical to the original.");
  }

  @Test
  void extractCertificateFromSaml_NoCertificateInXml_ShouldThrowException() throws Exception {
    // Arrange
    String xmlWithoutCert =
        """
      <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol">
          <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
          </saml2:Assertion>
      </saml2p:Response>
      """;
    Method parseMethod = SamlValidator.class.getDeclaredMethod("parseXmlDocument", String.class);
    parseMethod.setAccessible(true);
    Document doc = (Document) parseMethod.invoke(samlValidator, xmlWithoutCert);

    // Act & Assert
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              extractCertificateMethod.invoke(samlValidator, doc, "any-cert");
            });

    // The actual exception is SecurityException, wrapped in InvocationTargetException by reflection
    assertEquals(SecurityException.class, exception.getCause().getClass());
    assertEquals(
        "No X.509 certificate found in the SAML response", exception.getCause().getMessage());
  }

  @Test
  void extractCertificateFromSaml_CertificateMismatch_ShouldThrowException() throws Exception {
    // Arrange
    Document doc = createSamlDocumentWithCert(testCertificateBase64);
    String wrongCertificate = "a-different-certificate-string";

    // Act & Assert
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              extractCertificateMethod.invoke(samlValidator, doc, wrongCertificate);
            });

    assertEquals(SecurityException.class, exception.getCause().getClass());
    assertEquals("Incorrect certificate", exception.getCause().getMessage());
  }

  @Test
  void validateCertificate_ExpiredCertificate_ShouldThrowException() throws Exception {
    X509Certificate expiredCertificate = CertificateTestUtils.generateExpiredCertificate();

    // Act & Assert
    SecurityException exception =
        assertThrows(
            SecurityException.class,
            () -> samlValidator.validateCertificate(expiredCertificate),
            "An expired certificate should throw a SecurityException.");

    assertTrue(
        exception.getMessage().startsWith("Certificate expired on:"),
        "The exception message should indicate that the certificate is expired.");
  }
}
