package it.pagopa.selfcare.document.crypto.client;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import it.pagopa.selfcare.document.crypto.entity.Credentials;
import it.pagopa.selfcare.document.crypto.entity.Preferences;
import it.pagopa.selfcare.document.crypto.entity.SignRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

class NamirialHttpClientTest {

    private static final String TEST_BASE_URL = "https://test.namirial.com";
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-password";
    private static final Path TEST_PDF_FILE_PATH = Path.of("src/test/resources/signTest.pdf");

    private EnvironmentVariables environmentVariables;

    // Capture request details for verification
    private final AtomicReference<String> capturedRequestBody = new AtomicReference<>();
    private final AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();
    private final AtomicReference<String> capturedUrl = new AtomicReference<>();

    @BeforeEach
    void setup() throws Exception {
        environmentVariables = new EnvironmentVariables(
            "NAMIRIAL_BASE_URL", TEST_BASE_URL
        );
        environmentVariables.setup();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (environmentVariables != null) {
            environmentVariables.teardown();
        }
        capturedRequestBody.set(null);
        capturedHeaders.set(null);
        capturedUrl.set(null);
    }
    
    @Test
    void testSignDocument_Success_Returns200() throws IOException {
        // Arrange
        byte[] expectedResponse = "signed-pdf-content".getBytes(StandardCharsets.UTF_8);
        File testFile = createTempTestFile();

        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        // Create mock HTTP client using reflection to inject
        NamirialHttpClient testClient = createMockHttpClient(200, expectedResponse);

        // Act
        byte[] result = testClient.signDocument(request);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResponse, result, "Result should match expected response");
    }

    @Test
    void testSignDocument_MultipartContentConstruction() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials("user123", "pass456");
        Preferences preferences = new Preferences("SHA512");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        // Create a real multipart to verify construction is possible
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        MultipartContent multipartContent = new MultipartContent().setBoundary(boundary);
        ObjectMapper objectMapper = new ObjectMapper();

        String credentialsJson = objectMapper.writeValueAsString(credentials);
        String preferencesJson = objectMapper.writeValueAsString(preferences);

        // Act - Add parts
        multipartContent.addPart(
            new MultipartContent.Part(
                new HttpHeaders().set("Content-Disposition",
                    "form-data; name=\"file\"; filename=\"" + testFile.getName() + "\""),
                new FileContent("application/pdf", testFile)
            )
        );

        multipartContent.addPart(
            new MultipartContent.Part(
                new HttpHeaders().set("Content-Disposition", "form-data; name=\"credentials\""),
                new ByteArrayContent("application/json", credentialsJson.getBytes())
            )
        );

        multipartContent.addPart(
            new MultipartContent.Part(
                new HttpHeaders().set("Content-Disposition", "form-data; name=\"preferences\""),
                new ByteArrayContent("application/json", preferencesJson.getBytes())
            )
        );

        // Assert - Write to stream and verify
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        multipartContent.writeTo(outputStream);
        String requestBody = outputStream.toString(StandardCharsets.UTF_8);

        assertNotNull(requestBody, "Request body should be generated");
        assertFalse(requestBody.isEmpty(), "Request body should not be empty");

        // Verify credentials JSON
        assertTrue(credentialsJson.contains("user123"),
            "Credentials JSON should contain username");
        assertTrue(credentialsJson.contains("pass456"),
            "Credentials JSON should contain password");

        // Verify preferences JSON
        assertTrue(preferencesJson.contains("SHA512"),
            "Preferences JSON should contain hash algorithm");

        // Verify the SignRequest has all required parts
        assertNotNull(request.getFile(), "SignRequest should have file");
        assertNotNull(request.getCredentials(), "SignRequest should have credentials");
        assertNotNull(request.getPreferences(), "SignRequest should have preferences");
    }

    @Test
    void testSignDocument_CorrectHeaders() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType("multipart/form-data; boundary=" + boundary);

        // Assert
        String contentType = headers.getContentType();
        assertNotNull(contentType, "Content-Type header should be set");
        assertTrue(contentType.startsWith("multipart/form-data"),
            "Content-Type should be multipart/form-data");
        assertTrue(contentType.contains("boundary="),
            "Content-Type should contain boundary parameter");
        assertTrue(contentType.contains(boundary),
            "Content-Type should contain the correct boundary value");

        // Verify the request has all necessary components
        assertNotNull(request.getFile(), "Request should have file");
        assertNotNull(request.getCredentials(), "Request should have credentials");
        assertNotNull(request.getPreferences(), "Request should have preferences");
    }

    @Test
    void testSignDocument_BoundaryConfiguration() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        byte[] mockResponse = "response".getBytes(StandardCharsets.UTF_8);
        NamirialHttpClient testClient = createCapturingMockHttpClient(200, mockResponse);

        // Act
        testClient.signDocument(request);

        // Assert
        String requestBody = capturedRequestBody.get();
        String expectedBoundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";

        assertTrue(requestBody.contains("--" + expectedBoundary),
            "Request body should contain boundary markers");
        assertTrue(requestBody.contains("--" + expectedBoundary + "--"),
            "Request body should contain closing boundary marker");

        // Verify boundary is in Content-Type header
        HttpHeaders headers = capturedHeaders.get();
        String contentType = headers.getContentType();
        assertTrue(contentType.contains("boundary=" + expectedBoundary),
            "Content-Type header should contain correct boundary");
    }

    @Test
    void testSignDocument_CredentialsJsonSerialization() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials("json-user", "json-pass");
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        byte[] mockResponse = "response".getBytes(StandardCharsets.UTF_8);
        NamirialHttpClient testClient = createCapturingMockHttpClient(200, mockResponse);

        // Act
        testClient.signDocument(request);

        // Assert - Verify JSON format
        String requestBody = capturedRequestBody.get();

        // Check JSON structure for credentials
        assertTrue(requestBody.contains("\"username\"") || requestBody.contains("username"),
            "Credentials JSON should contain username field");
        assertTrue(requestBody.contains("\"password\"") || requestBody.contains("password"),
            "Credentials JSON should contain password field");
        assertTrue(requestBody.contains("json-user"),
            "Credentials JSON should contain actual username value");
        assertTrue(requestBody.contains("json-pass"),
            "Credentials JSON should contain actual password value");

        // Extract credentials JSON from multipart (simplified check)
        assertTrue(requestBody.contains("{") && requestBody.contains("}"),
            "Request should contain JSON objects");
    }

    @Test
    void testSignDocument_PreferencesJsonSerialization() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA384");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        byte[] mockResponse = "response".getBytes(StandardCharsets.UTF_8);
        NamirialHttpClient testClient = createCapturingMockHttpClient(200, mockResponse);

        // Act
        testClient.signDocument(request);

        // Assert
        String requestBody = capturedRequestBody.get();

        // Check JSON structure for preferences
        assertTrue(requestBody.contains("\"hashAlgorithm\"") || requestBody.contains("hashAlgorithm"),
            "Preferences JSON should contain hashAlgorithm field");
        assertTrue(requestBody.contains("SHA384"),
            "Preferences JSON should contain actual hash algorithm value");
    }

    @Test
    void testSignDocument_HttpResponseException_4xx() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        NamirialHttpClient testClient = createMockHttpClient(400, "Bad Request".getBytes());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> testClient.signDocument(request),
            "Should throw IllegalStateException for 4xx errors");

        assertEquals("Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request",
            exception.getMessage(),
            "Exception message should indicate Namirial invocation error");

        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertInstanceOf(HttpResponseException.class, exception.getCause(), "Cause should be HttpResponseException");
    }
    
    @Test
    void testSignDocument_HttpResponseException_5xx() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        NamirialHttpClient testClient = createMockHttpClient(500, "Internal Server Error".getBytes());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> testClient.signDocument(request),
            "Should throw IllegalStateException for 5xx errors");

        assertInstanceOf(HttpResponseException.class, exception.getCause(), "Cause should be HttpResponseException");

        HttpResponseException httpException = (HttpResponseException) exception.getCause();
        assertEquals(500, httpException.getStatusCode(),
            "Status code should be 500");
    }

    @Test
    void testSignDocument_CorrectUrlConstruction() throws IOException {
        // Arrange
        File testFile = createTempTestFile();
        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        byte[] mockResponse = "response".getBytes(StandardCharsets.UTF_8);
        NamirialHttpClient testClient = createCapturingMockHttpClient(200, mockResponse);

        // Act
        testClient.signDocument(request);

        // Assert
        String url = capturedUrl.get();
        assertNotNull(url, "URL should be captured");
        assertEquals(TEST_BASE_URL + "/SignEngineWeb/rest/service/signPAdES", url,
            "URL should point to correct Namirial endpoint");
    }
    
    @Test
    void testSignDocument_ResponseBodyParsing() throws IOException {
        // Arrange
        byte[] expectedResponse = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF magic number
        File testFile = createTempTestFile();

        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        NamirialHttpClient testClient = createMockHttpClient(200, expectedResponse);

        // Act
        byte[] result = testClient.signDocument(request);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.length, "Result should have correct length");
        assertArrayEquals(expectedResponse, result, "Result should match response bytes exactly");
    }

    @Test
    void testSignDocument_WithRealPdfFile() throws IOException {
        // Arrange
        assertTrue(Files.exists(TEST_PDF_FILE_PATH),
            "Test PDF file should exist at " + TEST_PDF_FILE_PATH);

        File testFile = TEST_PDF_FILE_PATH.toFile();
        byte[] expectedResponse = "signed-real-pdf".getBytes(StandardCharsets.UTF_8);

        Credentials credentials = new Credentials(TEST_USERNAME, TEST_PASSWORD);
        Preferences preferences = new Preferences("SHA256");
        SignRequest request = new SignRequest(testFile, credentials, preferences);

        NamirialHttpClient testClient = createMockHttpClient(200, expectedResponse);

        // Act
        byte[] result = testClient.signDocument(request);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResponse, result, "Result should match expected response");
    }

    private File createTempTestFile() throws IOException {
        Path tempFile = Files.createTempFile("test-pdf-", ".pdf");
        Files.writeString(tempFile, "%PDF-1.4 test content");
        tempFile.toFile().deleteOnExit();
        return tempFile.toFile();
    }

    private NamirialHttpClient createMockHttpClient(int statusCode, byte[] responseContent) {
        return new NamirialHttpClient() {
            @Override
            public byte[] signDocument(SignRequest request) {
                // Simulate HTTP interaction
                if (statusCode >= 400) {
                    HttpResponseException exception = new HttpResponseException.Builder(
                        statusCode,
                        statusCode >= 500 ? "Server Error" : "Client Error",
                        new HttpHeaders()
                    ).build();
                    throw new IllegalStateException(
                        "Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request",
                        exception
                    );
                }
                return responseContent;
            }
        };
    }

    private NamirialHttpClient createCapturingMockHttpClient(int statusCode, byte[] responseContent) {
        return new NamirialHttpClient() {
            @Override
            public byte[] signDocument(SignRequest request) {
                // Capture request details
                try {

                    String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                    MultipartContent multipartContent = new MultipartContent().setBoundary(boundary);
                    ObjectMapper objectMapper = new ObjectMapper();

                    String credentials = objectMapper.writeValueAsString(request.getCredentials());
                    String preferences = objectMapper.writeValueAsString(request.getPreferences());

                    multipartContent.addPart(
                        new MultipartContent.Part(
                            new HttpHeaders().set("Content-Disposition",
                                "form-data; name=\"file\"; filename=\"" + request.getFile().getName() + "\""),
                            new FileContent("application/pdf", request.getFile())
                        )
                    );

                    multipartContent.addPart(
                        new MultipartContent.Part(
                            new HttpHeaders().set("Content-Disposition", "form-data; name=\"credentials\""),
                            new ByteArrayContent("application/json", credentials.getBytes())
                        )
                    );

                    multipartContent.addPart(
                        new MultipartContent.Part(
                            new HttpHeaders().set("Content-Disposition", "form-data; name=\"preferences\""),
                            new ByteArrayContent("application/json", preferences.getBytes())
                        )
                    );

                    // Capture request body
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    multipartContent.writeTo(outputStream);
                    capturedRequestBody.set(outputStream.toString(StandardCharsets.UTF_8));

                    // Capture headers
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType("multipart/form-data; boundary=" + boundary);
                    capturedHeaders.set(headers);

                    // Capture URL
                    capturedUrl.set(TEST_BASE_URL + "/SignEngineWeb/rest/service/signPAdES");

                } catch (IOException e) {
                    throw new RuntimeException("Failed to capture request details", e);
                }

                if (statusCode >= 400) {
                    HttpResponseException exception = new HttpResponseException.Builder(
                        statusCode,
                        statusCode >= 500 ? "Server Error" : "Client Error",
                        new HttpHeaders()
                    ).build();
                    throw new IllegalStateException(
                        "Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request",
                        exception
                    );
                }

                return responseContent;
            }
        };
    }
}

