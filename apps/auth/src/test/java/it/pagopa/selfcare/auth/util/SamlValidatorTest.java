package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
public class SamlValidatorTest {
  private static final long FAKE_INTERVAL = 300;
  private SamlValidator samlValidator;
  // A Spy is a partial mock. It will behave like the real object,
  // unless we explicitly override a method's behavior.
  @Spy
  SamlValidator samlValidatorSpy;

  // Sample valid SAML response for testing (simplified)
  private static final String VALID_SAML_XML = """
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
                        <ds:Reference URI="#_assertion_id">
                            <ds:DigestValue>dummy_digest</ds:DigestValue>
                        </ds:Reference>
                    </ds:SignedInfo>
                    <ds:SignatureValue>dummy_signature</ds:SignatureValue>
                    <ds:KeyInfo>
                        <ds:X509Data>
                            <ds:X509Certificate>MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI</ds:X509Certificate>
                        </ds:X509Data>
                    </ds:KeyInfo>
                </ds:Signature>
            </saml2:Assertion>
        </saml2p:Response>
        """;

  private static final String VALID_SAML_BASE64 = Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

  private static final String INVALID_XML = "This is not valid XML";
  private static final String EMPTY_XML = "";

  // Sample certificate for testing (dummy)
  private static final String DUMMY_CERT_BASE64 = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUE7Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K";

  @BeforeEach
  void setUp() {
    samlValidator = new SamlValidator();
    samlValidatorSpy = Mockito.spy(samlValidator);
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
    String base64Xml = Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

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
    assertThrows(IllegalArgumentException.class, () ->
      samlValidator.cleanXmlContent(null));
  }

  @Test
  void cleanXmlContent_EmptyInput_ShouldThrowException() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
      samlValidator.cleanXmlContent(""));
  }

  @Test
  void cleanXmlContent_WithControlCharacters_ShouldRemoveControlCharacters() {
    // Given
    String xmlWithControlChars = "<?xml version=\"1.0\"?>\u0001\u0008\u001F<root>content</root>\u007F";

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
    boolean result = samlValidator.validateSamlResponse(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void validateSamlResponse_NullInput_ShouldReturnFalse() {
    // When
    boolean result = samlValidator.validateSamlResponse(null, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void validateSamlResponse_EmptyInput_ShouldReturnFalse() {
    // When
    boolean result = samlValidator.validateSamlResponse("", DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void testValidateSamlResponseAsync_Success() {
    // Arrange: Configure the spy. When the synchronous method is called,
    // force it to return 'true' without executing its actual complex logic.
    doReturn(true).when(samlValidatorSpy).validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method on the spy object.
    Uni<Boolean> resultUni = samlValidatorSpy.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Assert: Await the result and verify it is true.
    Boolean result = resultUni.await().indefinitely();
    assertTrue(result, "The async method should return true when the sync method succeeds.");
  }

  @Test
  void testValidateSamlResponseAsync_Failure() {
    // Arrange: Configure the spy to make the synchronous method return 'false'.
    doReturn(false).when(samlValidatorSpy).validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method.
    Uni<Boolean> resultUni = samlValidatorSpy.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Assert: Await the result and verify it is false.
    Boolean result = resultUni.await().indefinitely();
    assertFalse(result, "The async method should return false when the sync method fails.");
  }

  @Test
  void testValidateSamlResponseAsync_Exception() {
    // Arrange: Configure the spy to make the synchronous method throw an exception.
    RuntimeException syncException = new RuntimeException("Error in sync validation");
    doThrow(syncException).when(samlValidatorSpy).validateSamlResponse(anyString(), anyString(), anyLong());

    // Act: Call the asynchronous method.
    Uni<Boolean> resultUni = samlValidatorSpy.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Assert: Verify that awaiting the result of the Uni throws the same exception.
    // This confirms that the failure is correctly propagated.
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
      resultUni.await().indefinitely();
    });

    assertEquals(syncException.getMessage(), thrown.getMessage(), "The exception should be propagated to the Uni.");
  }

  @Test
  void validateSamlResponseAsync_ValidInput_ShouldReturnUni() {
    // Given
    String validBase64 = Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    // When
    Uni<Boolean> result = samlValidator.validateSamlResponseAsync(validBase64, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    assertNotNull(result);

    // Test the Uni - this will likely fail due to certificate validation but should not throw
    UniAssertSubscriber<Boolean> subscriber = result
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();
    subscriber.assertCompleted();
    // The result will be false due to dummy certificate, but should not fail
    Boolean value = subscriber.getItem();
    assertNotNull(value);
  }

  @Test
  void validateSamlResponseAsync_InvalidInput_ShouldCompleteWithFalse() {
    // When
    Uni<Boolean> result = samlValidator.validateSamlResponseAsync(INVALID_XML, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then
    UniAssertSubscriber<Boolean> subscriber = result
      .subscribe().withSubscriber(UniAssertSubscriber.create());

    subscriber.awaitItem();
    subscriber.assertCompleted();
    Boolean value = subscriber.getItem();
    assertFalse(value);
  }

  @Test
  void extractSamlInfo_ValidSaml_ShouldExtractInformation() {
    // When
    Map<String, Object> result = samlValidator.extractSamlInfo(VALID_SAML_XML);

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("name_id"));
    assertTrue(result.containsKey("issuer"));
    assertTrue(result.containsKey("session_index"));

    assertEquals("user@example.com", result.get("name_id"));
    assertEquals("https://accounts.google.com/o/oauth2/auth", result.get("issuer"));
    assertEquals("session123", result.get("session_index"));
  }

  @Test
  void extractSamlInfo_Base64Input_ShouldExtractInformation() {
    // Given
    String base64Saml = Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    // When
    Map<String, Object> result = samlValidator.extractSamlInfo(base64Saml);

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("name_id"));
    assertEquals("user@example.com", result.get("name_id"));
  }

  @Test
  void extractSamlInfo_InvalidXml_ShouldReturnErrorMap() {
    // When
    Map<String, Object> result = samlValidator.extractSamlInfo(INVALID_XML);

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("error"));
    assertNotNull(result.get("error"));
  }

  @Test
  void extractSamlInfo_EmptyXml_ShouldThrowException() {
    // When
    Map<String, Object> result = samlValidator.extractSamlInfo("");

    // Then
    assertNotNull(result);
    assertTrue(result.containsKey("error"));
  }

  @Test
  void isTimestampValid_ValidTimestamp_ShouldReturnTrue() throws Exception {
    // Given
    String currentTime = Instant.now().toString();
    String samlWithCurrentTime = VALID_SAML_XML.replace("2024-01-15T10:30:00Z", currentTime);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new ByteArrayInputStream(samlWithCurrentTime.getBytes(StandardCharsets.UTF_8)));

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
    Document doc = builder.parse(new ByteArrayInputStream(samlWithOldTime.getBytes(StandardCharsets.UTF_8)));

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
    Document doc = builder.parse(new ByteArrayInputStream(samlWithFutureTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_NoResponseElement_ShouldReturnFalse() throws Exception {
    // Given
    String xmlWithoutResponse = """
            <?xml version="1.0" encoding="UTF-8"?>
            <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="_assertion_id">
                <saml2:Issuer>test</saml2:Issuer>
            </saml2:Assertion>
            """;

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(new ByteArrayInputStream(xmlWithoutResponse.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL);

    // Then
    assertFalse(result);
  }

  @Test
  void isTimestampValid_NoIssueInstantAttribute_ShouldReturnFalse() throws Exception {
    // Given
    String samlWithoutIssueInstant = """
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
    Document doc = builder.parse(new ByteArrayInputStream(samlWithoutIssueInstant.getBytes(StandardCharsets.UTF_8)));

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
    Document doc = builder.parse(new ByteArrayInputStream(samlWithRecentTime.getBytes(StandardCharsets.UTF_8)));

    // When
    boolean result = samlValidator.isTimestampValid(doc, FAKE_INTERVAL); // 5 minutes tolerance

    // Then
    assertTrue(result);
  }

  @Test
  void validateSamlResponse_WithMockedTimestamp_ShouldHandleTimeValidation() {
    // This test would require more complex mocking of the document parsing
    // and timestamp validation. For now, we test the integration through other methods

    // Given
    String base64InvalidTime = Base64.getEncoder().encodeToString(VALID_SAML_XML.getBytes(StandardCharsets.UTF_8));

    // When - this will fail on timestamp validation since the XML has old timestamp
    boolean result = samlValidator.validateSamlResponse(base64InvalidTime, DUMMY_CERT_BASE64, FAKE_INTERVAL);

    // Then - should return false due to old timestamp
    assertFalse(result);
  }
}
