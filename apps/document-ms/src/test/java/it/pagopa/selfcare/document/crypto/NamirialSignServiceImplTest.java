package it.pagopa.selfcare.document.crypto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import it.pagopa.selfcare.document.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.document.crypto.entity.Credentials;
import it.pagopa.selfcare.document.crypto.entity.SignRequest;
import it.pagopa.selfcare.document.crypto.impl.NamiralSignServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

class NamirialSignServiceImplTest {

    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-password";
    private static final String TEST_PDF_CONTENT = "%PDF-1.4 test content";
    private static final Path TEST_PDF_FILE_PATH = Path.of("src/test/resources/signTest.pdf");

    private EnvironmentVariables environmentVariables;
    private MockedConstruction<NamirialHttpClient> mockedHttpClientConstruction;

    @BeforeEach
    void setup() throws Exception {
        // Setup environment variables for credentials
        environmentVariables = new EnvironmentVariables(
            "NAMIRIAL_SIGN_SERVICE_IDENTITY_USER", TEST_USERNAME,
            "NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD", TEST_PASSWORD
        );
        environmentVariables.setup();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (environmentVariables != null) {
            environmentVariables.teardown();
        }
        if (mockedHttpClientConstruction != null) {
            mockedHttpClientConstruction.close();
        }
    }

    @Test
    void testPkcs7Signhash_Success() throws IOException {
        // Arrange
        byte[] expectedResult = "signed-content".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        // Mock NamirialHttpClient construction and behavior
        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenReturn(expectedResult));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act
        byte[] result = service.pkcs7Signhash(inputStream);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResult, result, "Result should match expected signed content");

        // Verify NamirialHttpClient was constructed and used
        assertEquals(1, mockedHttpClientConstruction.constructed().size(),
            "NamirialHttpClient should be constructed once");

        NamirialHttpClient constructedClient = mockedHttpClientConstruction.constructed().get(0);
        verify(constructedClient, times(1)).signDocument(any(SignRequest.class));
    }

    @Test
    void testPkcs7Signhash_VerifySignRequestParameters() throws IOException {
        // Arrange
        byte[] expectedResult = "signed-content".getBytes(StandardCharsets.UTF_8);
        String testContent = "Test PDF content";
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<SignRequest> signRequestCaptor = ArgumentCaptor.forClass(SignRequest.class);

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenReturn(expectedResult));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act
        service.pkcs7Signhash(inputStream);

        // Assert
        NamirialHttpClient constructedClient = mockedHttpClientConstruction.constructed().get(0);
        verify(constructedClient).signDocument(signRequestCaptor.capture());

        SignRequest capturedRequest = signRequestCaptor.getValue();

        // Verify credentials
        assertNotNull(capturedRequest.getCredentials(), "Credentials should not be null");
        assertEquals(TEST_USERNAME, capturedRequest.getCredentials().getUsername(),
            "Username should match environment variable");
        assertEquals(TEST_PASSWORD, capturedRequest.getCredentials().getPassword(),
            "Password should match environment variable");

        // Verify preferences
        assertNotNull(capturedRequest.getPreferences(), "Preferences should not be null");
        assertEquals("SHA256", capturedRequest.getPreferences().getHashAlgorithm(),
            "Hash algorithm should be SHA256");

        // Verify file
        assertNotNull(capturedRequest.getFile(), "File should not be null");
        assertTrue(capturedRequest.getFile().exists(), "Temporary file should exist");
        assertTrue(capturedRequest.getFile().getName().startsWith("tempfile"),
            "File should have 'tempfile' prefix");
        assertTrue(capturedRequest.getFile().getName().endsWith(".pdf"),
            "File should have .pdf extension");

        // Verify file content
        String fileContent = Files.readString(capturedRequest.getFile().toPath());
        assertEquals(testContent, fileContent, "File content should match input stream content");
    }

    @Test
    void testPkcs7Signhash_HttpClientThrowsIOException() {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class)))
                .thenThrow(new IOException("Network error")));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.pkcs7Signhash(inputStream),
            "Should throw IllegalStateException when HTTP client fails");

        assertEquals("Something gone wrong when invoking Namirial in order to calculate pkcs7 hash sign request",
            exception.getMessage(),
            "Exception message should indicate Namirial invocation error");

        assertNotNull(exception.getCause(), "Exception should have a cause");
        assertInstanceOf(IOException.class, exception.getCause(), "Cause should be IOException");
        assertEquals("Network error", exception.getCause().getMessage(),
            "Cause message should be preserved");
    }

    @Test
    void testPkcs7Signhash_TempFileCreationFails() {
        // Arrange
        // Create an InputStream that will fail when read
        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Stream read error");
            }
        };

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> {
                // This won't be called due to earlier failure
            });

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.pkcs7Signhash(inputStream),
            "Should throw IllegalStateException when file operations fail");

        assertInstanceOf(IOException.class, exception.getCause(), "Cause should be IOException from file operations");
    }

    @Test
    void testPkcs7Signhash_TemporaryFileHandling() throws IOException {
        // Arrange
        byte[] expectedResult = "signed".getBytes(StandardCharsets.UTF_8);
        String pdfContent = "%PDF-1.4\nTest Document";
        InputStream inputStream = new ByteArrayInputStream(pdfContent.getBytes(StandardCharsets.UTF_8));

        final File[] capturedFile = new File[1];

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenAnswer(invocation -> {
                SignRequest request = invocation.getArgument(0);
                capturedFile[0] = request.getFile();
                return expectedResult;
            }));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act
        byte[] result = service.pkcs7Signhash(inputStream);

        // Assert
        assertNotNull(capturedFile[0], "File should have been captured");
        assertTrue(capturedFile[0].exists(), "Temporary file should exist during processing");

        // Read the temporary file content
        byte[] fileContent = Files.readAllBytes(capturedFile[0].toPath());
        assertArrayEquals(pdfContent.getBytes(StandardCharsets.UTF_8), fileContent,
            "Temporary file should contain exact input stream data");

        assertArrayEquals(expectedResult, result, "Result should be returned correctly");
    }

    @Test
    void testPkcs7Signhash_EmptyInputStream() {
        // Arrange
        byte[] expectedResult = "signed-empty".getBytes(StandardCharsets.UTF_8);
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenReturn(expectedResult));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act
        byte[] result = service.pkcs7Signhash(emptyStream);

        // Assert
        assertNotNull(result, "Result should not be null even with empty input");
        assertArrayEquals(expectedResult, result, "Result should be returned correctly");
    }

    @Test
    void testPkcs7Signhash_CredentialsFromEnvironment() throws IOException {
        // Arrange
        byte[] expectedResult = "signed".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenReturn(expectedResult));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();

        // Act
        service.pkcs7Signhash(inputStream);

        // Assert
        verify(mockedHttpClientConstruction.constructed().get(0)).signDocument(requestCaptor.capture());

        Credentials credentials = requestCaptor.getValue().getCredentials();
        assertEquals(TEST_USERNAME, credentials.getUsername(),
            "Username should come from NAMIRIAL_SIGN_SERVICE_IDENTITY_USER env var");
        assertEquals(TEST_PASSWORD, credentials.getPassword(),
            "Password should come from NAMIRIAL_SIGN_SERVICE_IDENTITY_PASSWORD env var");
    }

    @Test
    void testPkcs7Signhash_WithRealPdfFile() throws IOException {
        // Arrange
        byte[] expectedResult = "signed-real-pdf".getBytes(StandardCharsets.UTF_8);

        // Verify test file exists
        assertTrue(Files.exists(TEST_PDF_FILE_PATH),
            "Test PDF file should exist at " + TEST_PDF_FILE_PATH);

        mockedHttpClientConstruction = Mockito.mockConstruction(NamirialHttpClient.class,
            (mock, context) -> when(mock.signDocument(any(SignRequest.class))).thenReturn(expectedResult));

        NamiralSignServiceImpl service = new NamiralSignServiceImpl();
        ArgumentCaptor<SignRequest> requestCaptor = ArgumentCaptor.forClass(SignRequest.class);

        // Act
        byte[] result;
        try (FileInputStream fis = new FileInputStream(TEST_PDF_FILE_PATH.toFile())) {
            result = service.pkcs7Signhash(fis);
        }

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResult, result, "Result should match expected signed content");

        // Verify the SignRequest was created correctly
        verify(mockedHttpClientConstruction.constructed().get(0)).signDocument(requestCaptor.capture());

        SignRequest capturedRequest = requestCaptor.getValue();

        // Verify the temporary file was created with actual PDF content
        assertNotNull(capturedRequest.getFile(), "File should not be null");
        assertTrue(capturedRequest.getFile().exists(), "Temporary file should exist");

        // Verify file size matches original PDF
        long originalSize = Files.size(TEST_PDF_FILE_PATH);
        long tempFileSize = capturedRequest.getFile().length();
        assertEquals(originalSize, tempFileSize,
            "Temporary file size should match original PDF file size");

        // Verify file content is identical to the original PDF
        byte[] originalContent = Files.readAllBytes(TEST_PDF_FILE_PATH);
        byte[] tempFileContent = Files.readAllBytes(capturedRequest.getFile().toPath());
        assertArrayEquals(originalContent, tempFileContent,
            "Temporary file content should be identical to original PDF");

        // Verify credentials and preferences are set correctly
        assertEquals(TEST_USERNAME, capturedRequest.getCredentials().getUsername());
        assertEquals(TEST_PASSWORD, capturedRequest.getCredentials().getPassword());
        assertEquals("SHA256", capturedRequest.getPreferences().getHashAlgorithm());
    }
}

