package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SamlValidatorCertificateTest {

  @Inject
  SamlValidator samlValidator;

  private static final String FAKE_CERT = "-----BEGIN CERTIFICATE-----MIIDDzCCAfegAwIBAgIUTCG4Fbd1yUJgFMfL8ic1JIJji80wDQYJKoZIhvcNAQELBQAwFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MB4XDTI1MDkwOTE1MjA1N1oXDTI2MDkwOTE1MjA1N1owFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmk4NOiqV11om20yNg/Tpx10FaNO9fGUDtM14V0gh7e9meSHh/nLVSAK/7uAPGvWUL3xZ++0LUFLsArFLSHBsfull63w95Uoq3PtABqUKSkI2gkbYclzFiIdrrH9rIoMYvYG5zIroKNSD2/iUDfE3Go78AWilfFqa/OvIZ3riNpTyqtAbFyqJ9tBlSgdSVV9LrqdpTRnk92bkaX0DVHwnXay3z0RkWjO8uOD/GGC3B8dDuPUS4wZTwgfS8cguz9SsgI0hMi2RXRmeTiSrAJFlx14PfGKnBsaVnM+JDdZH9KTuSJ7+8d8ld4yWz1q56UVdTw1DIV8OC91mEvhtuYJGvwIDAQABo1MwUTAdBgNVHQ4EFgQUfGEOxC2NY2ONlCj7K8vcmh/utGYwHwYDVR0jBBgwFoAUfGEOxC2NY2ONlCj7K8vcmh/utGYwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEANiCG8BvlOuX9cS3yY4DxvLVv0gzTzwAo157JFFE50+qZ5Hn2vQKNKQOlrSHe9+5Jr2K/sGswRapDVG0o+wV5LlFTcoe6k/DVR196XSzg4Y60KTDQLvs/0dfSZ0tYjn0Sni5TuQrGfJgnwgZ2XkWUHMXKg22Fd6ig/PiZE+fNbyisTByUJlcqjESPP1CkhWhhbsg8VSmv66lyeWNSY7dEJbceG/Zl8z+SmSU2IkYQ2eQu0eOc7MvaaPZOWdubeJefm75nx6jnnQZMWEpL5+wQaB2qKdejRibX0ps0E6lRBOSUqf8Ij3BLMRQ4rPYAvM8eGxbsPRrR5KawuhrPCcU82w==-----END CERTIFICATE-----";
  private static final String CERT_EXPIRED = "-----BEGIN CERTIFICATE-----MIIDBTCCAe2gAwIBAgIUa8glZFUe5n1qdG7oSvrrQ+2dy/YwDQYJKoZIhvcNAQELBQAwEjEQMA4GA1UEAwwHdGVzdC5pbzAeFw0yNTA5MTAwODIwNDFaFw0yNTA5MTEwODIwNDFaMBIxEDAOBgNVBAMMB3Rlc3QuaW8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC0VjzXz16KMQUNbCsIrViXVCWiPXmtKrfwOO7zwkvFaWwQTQnBKpZlauAs3yl9yC6dakuAU6a+yZEbjZYON9OUqSIY0jh/qdMdphzumUsX3B2OhnQeTr3jfLz+1ic3usfzN6Hu7XXks1LJXtwjwm4i8vvn4GC2R7VrQpjhSMdRQRgSacB63nkZ35ZANPvZTAOpglDSaB3ctxtTu5Auc4SPQED4twgKt5p/RbBVkYmfadFbBA65hUzUYiLjfXDvRQDaF8MRovx+8F9fJQjqziVPbkrhv2q9MyO+OHFfBMtTv5bHVyt4fWyziASqqVYooop6prt0thWHPUVep1J2scIHAgMBAAGjUzBRMB0GA1UdDgQWBBQXBP0vLZvX+0HjxW7izXW8fB5QtjAfBgNVHSMEGDAWgBQXBP0vLZvX+0HjxW7izXW8fB5QtjAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAy20g2yY6UHsSxXYPCPzYDjEJ5QvdqSOmmiHDWvLP9yzeQ+/9NX39T2ID1wpg1ahnMrGdVm7euU7AMV8vL9dDWum6cVOOYcqBaEoJxsn4tCSZJNwYKpCHK0ghdN7r4uFj6X0fEmPGuTFF+CxlyrhfobJ9Odh5BhrIwoS6uKeVt7qaujalts6U86cA3PC30k3Zn7Z1kf7f+UxWz0wuSxEz3Tw/GU2ioyIr5Ec++MnDfMzf9AdRgRyJOXrv4+SN+qm8J2yUaqjRn2CDHqfbWUFi5jPWKdxyRpteiEMWok016N6Q3QbjDluddZGcZgc+EHqFYK9XhoC22xCsa1b4NaYQ4-----END CERTIFICATE-----";
  private Method extractCertificateMethod;
  private X509Certificate testCertificate;
  private String testCertificateBase64;

  // Helper class to generate self-signed certificates for testing
  private static class CertificateTestUtils {
    public static X509Certificate generateCertificate(String cert) throws CertificateException {
      String cleanedPem = cert
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .replaceAll("\\s", "");

      byte[] certificateBytes = Base64.getDecoder().decode(cleanedPem);

      // 3. Usa una CertificateFactory per generare l'oggetto certificato
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      InputStream is = new ByteArrayInputStream(certificateBytes);

      return (X509Certificate) factory.generateCertificate(is);
    }

    public static X509Certificate generateCertificate(Instant start, Instant end, KeyPair keyPair) throws Exception {
      // This is a simplified version of a self-signed certificate generator
      // In a real project, you might use a library like Bouncy Castle for more options
      String subjectDN = "CN=Test, O=TestOrg, C=IT";
      String command = String.format(
        "keytool -genkeypair -alias testcert -keyalg RSA -keysize 2048 -dname \"%s\" " +
          "-startdate %tF -validity %d -keystore test.keystore -storepass password -keypass password",
        subjectDN, Date.from(start), 1);

      Process process = Runtime.getRuntime().exec(command);
      process.waitFor();

      // Get the class loader
      ClassLoader classLoader = CertificateTestUtils.class.getClassLoader();

      InputStream keystoreStream = classLoader.getResourceAsStream("test.keystore");

      if (keystoreStream == null) {
        throw new FileNotFoundException("Keystore file 'test.keystore' not found in resources!");
      }

      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(keystoreStream, "password".toCharArray());

      return (X509Certificate) keyStore.getCertificate("miocert");
    }

    public static KeyPair generateKeyPair() throws Exception {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(2048);
      return kpg.generateKeyPair();
    }
  }

  @Test
  void validateCertificate_ValidCertificate_ShouldNotThrowException() throws Exception {
    X509Certificate validCertificate = CertificateTestUtils.generateCertificate(FAKE_CERT);

    // Act & Assert
    assertDoesNotThrow(() -> samlValidator.validateCertificate(validCertificate),
      "A valid certificate should not throw an exception.");
  }

  @BeforeEach
  void setUp() throws Exception {
    // Make the private method accessible for testing
    extractCertificateMethod = SamlValidator.class.getDeclaredMethod("extractCertificateFromSaml", Document.class, String.class);
    extractCertificateMethod.setAccessible(true);

    // Generate a valid certificate to use in tests
    KeyPair keyPair = CertificateTestUtils.generateKeyPair();
    Instant now = Instant.now();
    Date startDate = Date.from(now.minus(1, ChronoUnit.DAYS));
    Date endDate = Date.from(now.plus(1, ChronoUnit.DAYS));
    testCertificate = CertificateTestUtils.generateCertificate(FAKE_CERT); //"CN=test.com", startDate, endDate, keyPair
    testCertificateBase64 = Base64.getEncoder().encodeToString(testCertificate.getEncoded());
  }

  private Document createSamlDocumentWithCert(String certificateContent) throws Exception {
    String xml = String.format("""
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
      """, certificateContent);
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
    X509Certificate extractedCert = (X509Certificate) extractCertificateMethod.invoke(samlValidator, doc, testCertificateBase64);

    // Assert
    assertNotNull(extractedCert, "The extracted certificate should not be null.");
    assertEquals(testCertificate, extractedCert, "The extracted certificate should be identical to the original.");
  }

  @Test
  void extractCertificateFromSaml_NoCertificateInXml_ShouldThrowException() throws Exception {
    // Arrange
    String xmlWithoutCert = """
      <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol">
          <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
          </saml2:Assertion>
      </saml2p:Response>
      """;
    Method parseMethod = SamlValidator.class.getDeclaredMethod("parseXmlDocument", String.class);
    parseMethod.setAccessible(true);
    Document doc = (Document) parseMethod.invoke(samlValidator, xmlWithoutCert);

    // Act & Assert
    Exception exception = assertThrows(Exception.class, () -> {
      extractCertificateMethod.invoke(samlValidator, doc, "any-cert");
    });

    // The actual exception is SecurityException, wrapped in InvocationTargetException by reflection
    assertEquals(SecurityException.class, exception.getCause().getClass());
    assertEquals("No X.509 certificate found in the SAML response", exception.getCause().getMessage());
  }

  @Test
  void extractCertificateFromSaml_CertificateMismatch_ShouldThrowException() throws Exception {
    // Arrange
    Document doc = createSamlDocumentWithCert(testCertificateBase64);
    String wrongCertificate = "a-different-certificate-string";

    // Act & Assert
    Exception exception = assertThrows(Exception.class, () -> {
      extractCertificateMethod.invoke(samlValidator, doc, wrongCertificate);
    });

    assertEquals(SecurityException.class, exception.getCause().getClass());
    assertEquals("Incorrect certificate", exception.getCause().getMessage());
  }


  @Test
  void validateCertificate_ExpiredCertificate_ShouldThrowException() throws Exception {
    X509Certificate expiredCertificate = CertificateTestUtils.generateCertificate(CERT_EXPIRED);

    // Act & Assert
    SecurityException exception = assertThrows(SecurityException.class,
      () -> samlValidator.validateCertificate(expiredCertificate),
      "An expired certificate should throw a SecurityException.");

    assertTrue(exception.getMessage().startsWith("Certificate expired on:"),
      "The exception message should indicate that the certificate is expired.");
  }
}
