package it.pagopa.selfcare.auth.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@QuarkusTest
public class SamlValidatorTest {
  private static final long FAKE_INTERVAL = 300;
  private static final long FAKE_LONG_INTERVAL = 300000000;
  private SamlValidator samlValidator;
  // A Spy is a partial mock. It will behave like the real object,
  // unless we explicitly override a method's behavior.
  @Spy SamlValidator samlValidatorSpy;

  @Mock private Logger log; // Sample valid SAML response for testing (simplified)

  private static final String VALID_SAML_XML =
      """
    <?xml version="1.0" encoding="UTF-8"?>
    <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                     ID="_response_id"
                     Version="2.0"
                     IssueInstant="2024-01-15T10:30:00Z"
                     Destination="https://example.com/saml/acs">
        <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">https://accounts.google.com/o/oauth2/auth</saml2:Issuer>
        <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="_assertion_id" Version="2.0" IssueInstant="2024-01-15T10:30:00Z">
            <saml2:Issuer>https://accounts.google.com/o/oauth2/auth</saml2:Issuer>
            <saml2:Subject>
                <saml2:NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:persistent">user@example.com</saml2:NameID>
            </saml2:Subject>
            <saml2:AuthnStatement SessionIndex="session123">
                <saml2:AuthnContext>
                    <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml2:AuthnContextClassRef>
                </saml2:AuthnContext>
            </saml2:AuthnStatement>
            <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                <ds:SignedInfo>
                    <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
                                                                                         <ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
                    <ds:Reference URI="#_assertion_id">
                        <ds:DigestValue>dummy_digest</ds:DigestValue>
                    </ds:Reference>
                </ds:SignedInfo>
                <ds:SignatureValue>dummy_signature</ds:SignatureValue>
                <ds:KeyInfo>
                    <ds:X509Data>
                        <ds:X509Certificate>MIIDDzCCAfegAwIBAgIUTCG4Fbd1yUJgFMfL8ic1JIJji80wDQYJKoZIhvcNAQELBQAwFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MB4XDTI1MDkwOTE1MjA1N1oXDTI2MDkwOTE1MjA1N1owFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmk4NOiqV11om20yNg/Tpx10FaNO9fGUDtM14V0gh7e9meSHh/nLVSAK/7uAPGvWUL3xZ++0LUFLsArFLSHBsfull63w95Uoq3PtABqUKSkI2gkbYclzFiIdrrH9rIoMYvYG5zIroKNSD2/iUDfE3Go78AWilfFqa/OvIZ3riNpTyqtAbFyqJ9tBlSgdSVV9LrqdpTRnk92bkaX0DVHwnXay3z0RkWjO8uOD/GGC3B8dDuPUS4wZTwgfS8cguz9SsgI0hMi2RXRmeTiSrAJFlx14PfGKnBsaVnM+JDdZH9KTuSJ7+8d8ld4yWz1q56UVdTw1DIV8OC91mEvhtuYJGvwIDAQABo1MwUTAdBgNVHQ4EFgQUfGEOxC2NY2ONlCj7K8vcmh/utGYwHwYDVR0jBBgwFoAUfGEOxC2NY2ONlCj7K8vcmh/utGYwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEANiCG8BvlOuX9cS3yY4DxvLVv0gzTzwAo157JFFE50+qZ5Hn2vQKNKQOlrSHe9+5Jr2K/sGswRapDVG0o+wV5LlFTcoe6k/DVR196XSzg4Y60KTDQLvs/0dfSZ0tYjn0Sni5TuQrGfJgnwgZ2XkWUHMXKg22Fd6ig/PiZE+fNbyisTByUJlcqjESPP1CkhWhhbsg8VSmv66lyeWNSY7dEJbceG/Zl8z+SmSU2IkYQ2eQu0eOc7MvaaPZOWdubeJefm75nx6jnnQZMWEpL5+wQaB2qKdejRibX0ps0E6lRBOSUqf8Ij3BLMRQ4rPYAvM8eGxbsPRrR5KawuhrPCcU82w==</ds:X509Certificate>
                    </ds:X509Data>
                </ds:KeyInfo>
            </ds:Signature>
        </saml2:Assertion>
    </saml2p:Response>
    """;

  private static final String VALID_SAML_BASE64 =
      Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

  private static final String INVALID_XML = "This is not valid XML";
  private static final String EMPTY_XML = "";

  // Sample certificate for testing (dummy)
  private static final String DUMMY_CERT_BASE64 =
      "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tTUlJRER6Q0NBZmVnQXdJQkFnSVVUQ0c0RmJkMXlVSmdGTWZMOGljMUpJSmppODB3RFFZSktvWklodmNOQVFFTEJRQXdGekVWTUJNR0ExVUVBd3dNYldsdmMybDBieTUwWlhOME1CNFhEVEkxTURrd09URTFNakExTjFvWERUSTJNRGt3T1RFMU1qQTFOMW93RnpFVk1CTUdBMVVFQXd3TWJXbHZjMmwwYnk1MFpYTjBNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQW1rNE5PaXFWMTFvbTIweU5nL1RweDEwRmFOTzlmR1VEdE0xNFYwZ2g3ZTltZVNIaC9uTFZTQUsvN3VBUEd2V1VMM3haKyswTFVGTHNBckZMU0hCc2Z1bGw2M3c5NVVvcTNQdEFCcVVLU2tJMmdrYlljbHpGaUlkcnJIOXJJb01ZdllHNXpJcm9LTlNEMi9pVURmRTNHbzc4QVdpbGZGcWEvT3ZJWjNyaU5wVHlxdEFiRnlxSjl0QmxTZ2RTVlY5THJxZHBUUm5rOTJia2FYMERWSHduWGF5M3owUmtXak84dU9EL0dHQzNCOGREdVBVUzR3WlR3Z2ZTOGNndXo5U3NnSTBoTWkyUlhSbWVUaVNyQUpGbHgxNFBmR0tuQnNhVm5NK0pEZFpIOUtUdVNKNys4ZDhsZDR5V3oxcTU2VVZkVHcxRElWOE9DOTFtRXZodHVZSkd2d0lEQVFBQm8xTXdVVEFkQmdOVkhRNEVGZ1FVZkdFT3hDMk5ZMk9ObENqN0s4dmNtaC91dEdZd0h3WURWUjBqQkJnd0ZvQVVmR0VPeEMyTlkyT05sQ2o3Szh2Y21oL3V0R1l3RHdZRFZSMFRBUUgvQkFVd0F3RUIvekFOQmdrcWhraUc5dzBCQVFzRkFBT0NBUUVBTmlDRzhCdmxPdVg5Y1MzeVk0RHh2TFZ2MGd6VHp3QW8xNTdKRkZFNTArcVo1SG4ydlFLTktRT2xyU0hlOSs1SnIySy9zR3N3UmFwRFZHMG8rd1Y1TGxGVGNvZTZrL0RWUjE5NlhTemc0WTYwS1REUUx2cy8wZGZTWjB0WWpuMFNuaTVUdVFyR2ZKZ253Z1oyWGtXVUhNWEtnMjJGZDZpZy9QaVpFK2ZOYnlpc1RCeVVKbGNxakVTUFAxQ2toV2hoYnNnOFZTbXY2Nmx5ZVdOU1k3ZEVKYmNlRy9abDh6K1NtU1UySWtZUTJlUXUwZU9jN012YWFQWk9XZHViZUplZm03NW54NmpublFaTVdFcEw1K3dRYUIycUtkZWpSaWJYMHBzMEU2bFJCT1NVcWY4SWozQkxNUlE0clBZQXZNOGVHeGJzUFJyUjVLYXd1aHJQQ2NVODJ3PT0tLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0t";
  private static final String FAKE_CERT =
      "-----BEGIN CERTIFICATE-----MIIDDzCCAfegAwIBAgIUTCG4Fbd1yUJgFMfL8ic1JIJji80wDQYJKoZIhvcNAQELBQAwFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MB4XDTI1MDkwOTE1MjA1N1oXDTI2MDkwOTE1MjA1N1owFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmk4NOiqV11om20yNg/Tpx10FaNO9fGUDtM14V0gh7e9meSHh/nLVSAK/7uAPGvWUL3xZ++0LUFLsArFLSHBsfull63w95Uoq3PtABqUKSkI2gkbYclzFiIdrrH9rIoMYvYG5zIroKNSD2/iUDfE3Go78AWilfFqa/OvIZ3riNpTyqtAbFyqJ9tBlSgdSVV9LrqdpTRnk92bkaX0DVHwnXay3z0RkWjO8uOD/GGC3B8dDuPUS4wZTwgfS8cguz9SsgI0hMi2RXRmeTiSrAJFlx14PfGKnBsaVnM+JDdZH9KTuSJ7+8d8ld4yWz1q56UVdTw1DIV8OC91mEvhtuYJGvwIDAQABo1MwUTAdBgNVHQ4EFgQUfGEOxC2NY2ONlCj7K8vcmh/utGYwHwYDVR0jBBgwFoAUfGEOxC2NY2ONlCj7K8vcmh/utGYwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEANiCG8BvlOuX9cS3yY4DxvLVv0gzTzwAo157JFFE50+qZ5Hn2vQKNKQOlrSHe9+5Jr2K/sGswRapDVG0o+wV5LlFTcoe6k/DVR196XSzg4Y60KTDQLvs/0dfSZ0tYjn0Sni5TuQrGfJgnwgZ2XkWUHMXKg22Fd6ig/PiZE+fNbyisTByUJlcqjESPP1CkhWhhbsg8VSmv66lyeWNSY7dEJbceG/Zl8z+SmSU2IkYQ2eQu0eOc7MvaaPZOWdubeJefm75nx6jnnQZMWEpL5+wQaB2qKdejRibX0ps0E6lRBOSUqf8Ij3BLMRQ4rPYAvM8eGxbsPRrR5KawuhrPCcU82w==-----END CERTIFICATE-----";

  private Method validateSignatureMethod;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    samlValidator = new SamlValidator();
    samlValidatorSpy = Mockito.spy(samlValidator);

    validateSignatureMethod =
        SamlValidator.class.getDeclaredMethod("validateSignature", Document.class, PublicKey.class);
    validateSignatureMethod.setAccessible(true);

    // Initialize the XML Security library
    if (!org.apache.xml.security.Init.isInitialized()) {
      org.apache.xml.security.Init.init();
    }
  }

  @Test
  void cleanXmlContent_ValidXml_ShouldReturnCleanedXml() {
    // Given
    String xmlWithBom = "\uFEFF  " + VALID_SAML_XML + "  ";

    // When
    String result = samlValidator.cleanXmlContent(xmlWithBom);

    // Then
    assertNotNull(result);
    assertFalse(result.startsWith("\uFEFF"));
    assertTrue(result.startsWith("<?xml"));
    assertEquals(VALID_SAML_XML.trim(), result);
  }

  @Test
  void cleanXmlContent_Base64EncodedXml_ShouldDecodeAndClean() {
    // Given
    String base64Xml =
        Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    // When
    String result = samlValidator.cleanXmlContent(base64Xml);

    // Then
    assertNotNull(result);
    assertTrue(result.startsWith("<?xml"));
    assertEquals(VALID_SAML_XML.trim(), result);
  }

  @Test
  void cleanXmlContent_InvalidBase64_ShouldUseOriginalContent() {
    // Given
    String invalidBase64 = "This is not base64!@#$";

    // When
    String result = samlValidator.cleanXmlContent(invalidBase64);

    // Then
    assertEquals(invalidBase64, result);
  }

  @Test
  void cleanXmlContent_NullInput_ShouldThrowException() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> samlValidator.cleanXmlContent(null));
  }

  @Test
  void cleanXmlContent_EmptyInput_ShouldThrowException() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> samlValidator.cleanXmlContent(""));
  }

  @Test
  void cleanXmlContent_WithControlCharacters_ShouldRemoveControlCharacters() {
    // Given
    String xmlWithControlChars =
        "<?xml version=\"1.0\"?>\u0001\u0008\u001F<root>content</root>\u007F";

    // When
    String result = samlValidator.cleanXmlContent(xmlWithControlChars);

    // Then
    assertFalse(result.contains("\u0001"));
    assertFalse(result.contains("\u0008"));
    assertFalse(result.contains("\u001F"));
    assertFalse(result.contains("\u007F"));
    assertTrue(result.contains("<?xml version=\"1.0\"?><root>content</root>"));
  }

  @Test
  void validateSamlResponse_InvalidInput_ShouldReturnFalse() {
    // When
    RuntimeException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              samlValidator.validateSamlResponse(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);
            });

    // Then
    assertEquals(
        "Illegal base64 character 20",
        thrown.getMessage(),
        "The exception message should be propagated");
  }

  @Test
  void validateSamlResponse_NullInput_ShouldReturnFalse() {
    // When
    RuntimeException thrown =
        assertThrows(
            NullPointerException.class,
            () -> {
              samlValidator.validateSamlResponse(null, DUMMY_CERT_BASE64, FAKE_INTERVAL);
            });
  }

  @Test
  void validateSamlResponse_EmptyInput_ShouldReturnFalse() {
    // When
    RuntimeException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              samlValidator.validateSamlResponse("", DUMMY_CERT_BASE64, FAKE_INTERVAL);
            });

    // Then
    assertEquals(
        "XML content is null or empty",
        thrown.getMessage(),
        "The exception message should be propagated");
  }

  @Test
  void testValidateSamlResponseAsync_cert_Failure() throws Exception {
    // Arrange: Configure the spy. When the synchronous method is called,
    // force it to return 'true' without executing its actual complex logic.
    doReturn(true).when(samlValidatorSpy).validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method on the spy object.
    Uni<Map<String, String>> resultUni =
        samlValidatorSpy.validateSamlResponseAsync(
            Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes()),
            DUMMY_CERT_BASE64,
            FAKE_LONG_INTERVAL);

    // Assert: Await the result and verify it is true.
    //    Boolean result = resultUni.await().indefinitely();
    //    assertFalse(result, "Signature validation failed");

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> {
              resultUni.await().indefinitely();
            });

    assertEquals(
        "Signature validation failed",
        thrown.getMessage(),
        "The exception should be propagated to the Uni.");
  }

  @Test
  void testValidateSamlResponseAsync_Failure() throws Exception {
    // Arrange: Configure the spy to make the synchronous method return 'false'.
    doReturn(false)
        .when(samlValidatorSpy)
        .validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method.
    Uni<Map<String, String>> resultUni =
        samlValidatorSpy.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Assert: Await the result and verify it is false.
    //    Boolean result = resultUni.await().indefinitely();
    //    assertFalse(result, "The async method should return false when the sync method fails.");

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> {
              resultUni.await().indefinitely();
            });

    assertEquals(
        "Illegal base64 character 20",
        thrown.getMessage(),
        "The exception should be propagated to the Uni.");
  }

  @Test
  void testValidateSamlResponseAsync_Exception() throws Exception {
    // Arrange: Configure the spy to make the synchronous method throw an exception.
    RuntimeException syncException = new RuntimeException("Illegal base64 character 20");
    doThrow(syncException)
        .when(samlValidatorSpy)
        .validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method.
    Uni<Map<String, String>> resultUni =
        samlValidatorSpy.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Assert: Verify that awaiting the result of the Uni throws the same exception.
    // This confirms that the failure is correctly propagated.
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> {
              resultUni.await().indefinitely();
            });

    assertEquals(
        syncException.getMessage(),
        thrown.getMessage(),
        "The exception should be propagated to the Uni.");
  }

  //  @Test
  //  void validateSamlResponseAsync_ValidInput_ShouldReturnUni() throws Exception {
  //    // Given
  //    String validBase64 =
  // Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));
  //
  //    // When
  //    Uni<Boolean> result = samlValidator.validateSamlResponseAsync(validBase64,
  // DUMMY_CERT_BASE64, FAKE_LONG_INTERVAL);
  //
  //    // Then
  //    assertNotNull(result);
  //
  //    // Test the Uni - this will likely fail due to certificate validation but should not throw
  //    UniAssertSubscriber<Boolean> subscriber = result
  //      .subscribe().withSubscriber(UniAssertSubscriber.create());
  //
  //    subscriber.awaitItem();
  //    subscriber.assertCompleted();
  //    // The result will be false due to dummy certificate, but should not fail
  //    Boolean value = subscriber.getItem();
  //    assertNotNull(value);
  //  }
  //
  @Test
  void validateSamlResponseAsync_InvalidInput_ShouldCompleteWithFalse() throws Exception {
    System.out.println("=== TEST INVALID XML ===");
    Uni<Map<String, String>> result =
        samlValidator.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    UniAssertSubscriber<Map<String, String>> subscriber =
        result.subscribe().withSubscriber(UniAssertSubscriber.create());
    subscriber
        .awaitFailure(Duration.ofSeconds(5))
        .assertFailedWith(IllegalArgumentException.class, "Illegal base64 character 20");
  }

  @Test
  void extractSamlInfo_ValidSaml_ShouldExtractInformation() throws Exception {
    String cleanedXml = samlValidator.cleanXmlContent(VALID_SAML_XML);
    Document doc = samlValidator.parseXmlDocument(cleanedXml);

    // When
    Map<String, String> result = samlValidator.extractSamlInfo(doc);

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("internal_id"));

    assertEquals("user@example.com", result.get("internal_id"));
  }

  @Test
  void extractSamlInfo_Base64Input_ShouldExtractInformation() throws Exception {
    // Given
    String base64Saml =
        Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    String cleanedXml = samlValidator.cleanXmlContent(base64Saml);
    Document doc = samlValidator.parseXmlDocument(cleanedXml);

    // When
    Map<String, String> result = samlValidator.extractSamlInfo(doc);

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("internal_id"));
    assertEquals("user@example.com", result.get("internal_id"));
  }

  @Test
  public void testEmptyXml() {
    System.out.println("=== TEST EMPTY XML ===");

    RuntimeException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              samlValidator.validateSamlResponse("", "", 190);
            });

    assertEquals(
        "XML content is null or empty",
        thrown.getMessage(),
        "The exception message should be propagated");
  }

  //  @Test apz
  //  void extractSamlInfo_InvalidXml_ShouldReturnErrorMap() throws Exception {
  //    String cleanedXml = samlValidator.cleanXmlContent(INVALID_XML);
  //    Document doc = samlValidator.parseXmlDocument(cleanedXml);
  //
  //    // When
  //    Map<String, Object> result = samlValidator.extractSamlInfo(doc);
  //
  //    // Then
  //    assertNotNull(result);
  //    assertTrue(result.containsKey("error"));
  //    assertNotNull(result.get("error"));
  //  }

  //  @Test apz
  //  void extractSamlInfo_EmptyXml_ShouldThrowException() throws Exception {
  //    String cleanedXml = samlValidator.cleanXmlContent(VALID_SAML_XML);
  //    Document doc = samlValidator.parseXmlDocument(cleanedXml);
  //
  //    // When
  //    Map<String, Object> result = samlValidator.extractSamlInfo(doc);
  //
  //    // Then
  //    assertNotNull(result);
  //    assertTrue(result.containsKey("error"));
  //  }

  @Test
  void isTimestampValid_ValidTimestamp_ShouldReturnTrue() throws Exception {
    // Given
    String currentTime = Instant.now().toString();
    String samlWithCurrentTime = VALID_SAML_XML.replace("2024-01-15T10:30:00Z", currentTime);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(
            new ByteArrayInputStream(samlWithCurrentTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertTrue(result);
  }

  @Test
  void isTimestampValid_OldTimestamp_ShouldReturnFalse() throws Exception {
    // Given - timestamp from 1 hour ago
    String oldTime = Instant.now().minus(Duration.ofHours(1)).toString();
    String samlWithOldTime = VALID_SAML_XML.replace("2024-01-15T10:30:00Z", oldTime);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(new ByteArrayInputStream(samlWithOldTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_FutureTimestamp_ShouldReturnFalse() throws Exception {
    // Given - timestamp 1 hour in the future
    String futureTime = Instant.now().plus(Duration.ofHours(1)).toString();
    String samlWithFutureTime = VALID_SAML_XML.replace("2024-01-15T10:30:00Z", futureTime);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(
            new ByteArrayInputStream(samlWithFutureTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_NoResponseElement_ShouldReturnFalse() throws Exception {
    // Given
    String xmlWithoutResponse =
        """
      <?xml version="1.0" encoding="UTF-8"?>
      <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="_assertion_id">
          <saml2:Issuer>test</saml2:Issuer>
      </saml2:Assertion>
      """;

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(
            new ByteArrayInputStream(xmlWithoutResponse.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_NoIssueInstantAttribute_ShouldReturnFalse() throws Exception {
    // Given
    String samlWithoutIssueInstant =
        """
      <?xml version="1.0" encoding="UTF-8"?>
      <saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
                       ID="_response_id"
                       Version="2.0">
          <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">test</saml2:Issuer>
      </saml2p:Response>
      """;

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(
            new ByteArrayInputStream(samlWithoutIssueInstant.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_WithinTolerance_ShouldReturnTrue() throws Exception {
    // Given - timestamp 2 minutes ago, tolerance is 5 minutes
    String recentTime = Instant.now().minus(Duration.ofMinutes(2)).toString();
    String samlWithRecentTime = VALID_SAML_XML.replace("2024-01-15T10:30:00Z", recentTime);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc =
        builder.parse(
            new ByteArrayInputStream(samlWithRecentTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertTrue(result);
  }

  @Test
  void validateSamlResponse_WithMockedTimestamp_ShouldHandleTimeValidation() throws Exception {
    // This test would require more complex mocking of the document parsing
    // and timestamp validation. For now, we test the integration through other methods

    // Given
    String base64InvalidTime =
        Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    // When - this will fail on timestamp validation since the XML has old timestamp
    RuntimeException thrown =
        assertThrows(
            SamlSignatureException.class,
            () -> {
              samlValidator.validateSamlResponse(
                  base64InvalidTime, DUMMY_CERT_BASE64, FAKE_INTERVAL);
            });

    assertEquals(
        "SAML validation failed",
        thrown.getMessage(),
        "The exception message should be propagated");
  }

  @Test
  public void testSamlValidation() {
    System.out.println("=== TEST SAML VALIDATION ===");

    try {
      String cleanedXml = samlValidator.cleanXmlContent(VALID_SAML_XML);
      Document doc = samlValidator.parseXmlDocument(cleanedXml);

      // When
      Map<String, String> info = samlValidator.extractSamlInfo(doc);

      System.out.println("Extracted information:");
      info.forEach((key, value) -> System.out.println("  " + key + ": " + value));

      assertTrue(info.containsKey("internal_id"), "Should contain internal_id");
      assertEquals("user@example.com", info.get("internal_id"));

    } catch (Exception e) {
      System.out.println("✗ Error during validation: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Test
  void validateSignature_WithoutAssertion_ShouldReturnFalse() throws Exception {
    // Arrange
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    Element root = doc.createElement("Root");
    doc.appendChild(root);
    PublicKey dummyKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

    // Act
    RuntimeException thrown =
        assertThrows(
            SamlSignatureException.class,
            () -> {
              samlValidator.validateSignature(doc, dummyKey);
            });
  }

  @Test
  void testFromBase64_ValidCertWithoutHeaders() throws Exception {
    // Certificato di esempio Base64 (codifica di "TEST_CERT_CONTENT")
    String rawCertContent = "TEST_CERT_CONTENT";
    String base64Cert = Base64.getEncoder().encodeToString(rawCertContent.getBytes());

    String result = samlValidator.fromBase64(base64Cert);

    assertNotNull(result);
    assertEquals(rawCertContent, result);
  }

  @Test
  void testFromBase64_NullString_ShouldThrowException() {
    String nullCert = null;

    // Ci aspettiamo che lanci NullPointerException, perché Base64.getDecoder().decode(null) lo fa
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          samlValidator.fromBase64(nullCert);
        });
  }

  @Test
  void testFromBase64_InvalidBase64Character_ShouldThrowException() {
    // Carattere '$' non è valido per Base64 standard
    String invalidBase64 = "someInvalid$Base64String";

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          samlValidator.fromBase64(invalidBase64);
        });
  }

  @Test
  void testFromBase64_Base64WithExtraSpacesBeforeDecoding_ShouldThrowException() throws Exception {
    String rawCertContent = "TEST";
    String base64Cert = Base64.getEncoder().encodeToString(rawCertContent.getBytes());

    // Aggiungiamo spazi interni che il decoder non dovrebbe gestire se non puliti
    String base64WithInternalSpaces = base64Cert.substring(0, 2) + " " + base64Cert.substring(2);

    // Il tuo metodo non pulisce gli spazi prima della decodifica
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          samlValidator.fromBase64(base64WithInternalSpaces);
        });
  }

  @Test
  void testFromBase64_Base64WithTrailingWhitespace_ShouldThrowException() throws Exception {
    String rawCertContent = "TEST";
    String base64Cert = Base64.getEncoder().encodeToString(rawCertContent.getBytes());

    // Aggiungiamo spazi alla fine della stringa Base64
    String base64WithTrailingSpaces = base64Cert + "   ";

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          samlValidator.fromBase64(base64WithTrailingSpaces);
        });
  }

  private Document createTestDocument(String xmlString) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xmlString)));
  }

  @Nested
  @DisplayName("Tests for Issuer, SessionIndex, and TextContent Extraction")
  class ExtractionTests {

    @Test
    @DisplayName("extractIssuer should return issuer when present")
    void extractIssuer_whenIssuerPresent() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:Issuer>https://idp.example.com</saml2:Issuer>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Optional<String> issuer = samlValidator.extractIssuer(doc);
      assertTrue(issuer.isPresent());
      assertEquals("https://idp.example.com", issuer.get());
    }

    @Test
    @DisplayName("extractIssuer should return empty when issuer is missing")
    void extractIssuer_whenIssuerMissing() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"></saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Optional<String> issuer = samlValidator.extractIssuer(doc);
      assertTrue(issuer.isEmpty());
    }

    @Test
    @DisplayName("extractSessionIndex should return index when present")
    void extractSessionIndex_whenPresent() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AuthnStatement SessionIndex=\"_abc123456789\">"
              + "</saml2:AuthnStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Optional<String> sessionIndex = samlValidator.extractSessionIndex(doc);
      assertTrue(sessionIndex.isPresent());
      assertEquals("_abc123456789", sessionIndex.get());
    }

    @Test
    @DisplayName("extractSessionIndex should return empty when attribute is missing")
    void extractSessionIndex_whenAttributeMissing() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AuthnStatement>"
              + "</saml2:AuthnStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Optional<String> sessionIndex = samlValidator.extractSessionIndex(doc);
      assertTrue(sessionIndex.isEmpty());
    }

    @Test
    @DisplayName("extractSessionIndex should return empty when attribute is blank")
    void extractSessionIndex_whenAttributeIsBlank() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AuthnStatement SessionIndex=\"\">"
              + "</saml2:AuthnStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Optional<String> sessionIndex = samlValidator.extractSessionIndex(doc);
      assertTrue(sessionIndex.isEmpty());
    }
  }

  @Nested
  @DisplayName("Tests for Attribute Extraction")
  class AttributeExtractionTests {

    @Test
    @DisplayName("extractAttributes should get a single attribute with a single value")
    void extractAttributes_singleAttributeSingleValue() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement>"
              + "<saml2:Attribute Name=\"email\">"
              + "<saml2:AttributeValue>test@example.com</saml2:AttributeValue>"
              + "</saml2:Attribute>"
              + "</saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertNotNull(attributes);
      assertEquals(1, attributes.size());
      assertEquals("test@example.com", attributes.get("email"));
    }

    @Test
    @DisplayName("extractAttributes should get multiple attributes")
    void extractAttributes_multipleAttributes() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement>"
              + "<saml2:Attribute Name=\"email\">"
              + "<saml2:AttributeValue>test@example.com</saml2:AttributeValue>"
              + "</saml2:Attribute>"
              + "<saml2:Attribute Name=\"firstName\">"
              + "<saml2:AttributeValue>John</saml2:AttributeValue>"
              + "</saml2:Attribute>"
              + "</saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertEquals(2, attributes.size());
      assertEquals("test@example.com", attributes.get("email"));
      assertEquals("John", attributes.get("firstName"));
    }

    @Test
    @DisplayName("extractAttributes should join multiple values with a comma")
    void extractAttributes_multipleValues() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement>"
              + "<saml2:Attribute Name=\"groups\">"
              + "<saml2:AttributeValue>Admin</saml2:AttributeValue>"
              + "<saml2:AttributeValue>User</saml2:AttributeValue>"
              + "</saml2:Attribute>"
              + "</saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertEquals(1, attributes.size());
      assertEquals("Admin,User", attributes.get("groups"));
    }

    @Test
    @DisplayName("extractAttributes should return empty map when no attributes are present")
    void extractAttributes_noAttributes() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement></saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertTrue(attributes.isEmpty());
    }

    @Test
    @DisplayName("extractAttributes should ignore attribute if Name is missing")
    void extractAttributes_missingName() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement>"
              + "<saml2:Attribute>"
              + // No 'Name' attribute
              "<saml2:AttributeValue>SomeValue</saml2:AttributeValue>"
              + "</saml2:Attribute>"
              + "</saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertTrue(attributes.isEmpty());
    }

    @Test
    @DisplayName("extractAttributes should ignore attribute if value is missing")
    void extractAttributes_missingValue() throws Exception {
      String xml =
          "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
              + "<saml2:AttributeStatement>"
              + "<saml2:Attribute Name=\"emptyAttribute\"></saml2:Attribute>"
              + "</saml2:AttributeStatement>"
              + "</saml2:Assertion>";
      Document doc = createTestDocument(xml);
      Map<String, String> attributes = samlValidator.extractAttributes(doc);
      assertTrue(attributes.isEmpty());
    }
  }
}
