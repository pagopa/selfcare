package it.pagopa.selfcare.onboarding.crypto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NamirialPkcs7HashSignServiceImplTest {

    private static final Path TEST_PDF_FILE_PATH = Path.of("src/test/resources/signTest.pdf");
    private static final String TEST_PDF_CONTENT = "%PDF-1.4 test content";

    private NamirialSignService mockNamirialSignService;
    private NamirialPkcs7HashSignServiceImpl service;

    @BeforeEach
    void setup() {
        mockNamirialSignService = mock(NamirialSignService.class);
        service = new NamirialPkcs7HashSignServiceImpl(mockNamirialSignService);
    }

    @Test
    void testSign_DelegatesToNamirialSignService() {
        // Arrange
        byte[] expectedResult = "signed-content".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenReturn(expectedResult);

        // Act
        byte[] result = service.sign(inputStream);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResult, result, "Result should match the value returned by NamirialSignService");

        // Verify delegation
        verify(mockNamirialSignService, times(1)).pkcs7Signhash(inputStream);
        verifyNoMoreInteractions(mockNamirialSignService);
    }

    @Test
    void testReturnsFullPdf_ReturnsTrue() {
        // Act
        boolean result = service.returnsFullPdf();

        // Assert
        assertTrue(result, "returnsFullPdf() should return true for Namirial implementation");
    }

    @Test
    void testSign_PassesInputStreamCorrectly() {
        // Arrange
        byte[] expectedResult = "result".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenReturn(expectedResult);

        // Act
        service.sign(inputStream);

        // Assert
        verify(mockNamirialSignService).pkcs7Signhash(inputStreamCaptor.capture());

        InputStream capturedInputStream = inputStreamCaptor.getValue();
        assertSame(inputStream, capturedInputStream,
            "The exact same InputStream instance should be passed to NamirialSignService");
    }

    @Test
    void testSign_PropagatesRuntimeException() {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));
        RuntimeException expectedException = new IllegalStateException("Namirial service error");

        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenThrow(expectedException);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> service.sign(inputStream),
            "Should propagate exception from NamirialSignService");

        assertEquals("Namirial service error", exception.getMessage(),
            "Exception message should be preserved");

        verify(mockNamirialSignService, times(1)).pkcs7Signhash(inputStream);
    }

    @Test
    void testSign_WithEmptyInputStream() {
        // Arrange
        byte[] expectedResult = "signed-empty".getBytes(StandardCharsets.UTF_8);
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);

        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenReturn(expectedResult);

        // Act
        byte[] result = service.sign(emptyStream);

        // Assert
        assertNotNull(result, "Result should not be null even with empty input");
        assertArrayEquals(expectedResult, result, "Result should be returned correctly");

        verify(mockNamirialSignService).pkcs7Signhash(emptyStream);
    }

    @Test
    void testSign_WithRealPdfFile() throws IOException {
        // Arrange
        byte[] expectedResult = "signed-real-pdf".getBytes(StandardCharsets.UTF_8);

        // Verify test file exists
        assertTrue(Files.exists(TEST_PDF_FILE_PATH),
            "Test PDF file should exist at " + TEST_PDF_FILE_PATH);

        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenReturn(expectedResult);

        // Act
        byte[] result;
        try (FileInputStream fis = new FileInputStream(TEST_PDF_FILE_PATH.toFile())) {
            result = service.sign(fis);
        }

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(expectedResult, result, "Result should match expected signed content");

        // Verify NamirialSignService was called with an InputStream
        verify(mockNamirialSignService, times(1)).pkcs7Signhash(any(InputStream.class));
    }

    @Test
    void testSign_VerifyResultEncoding() throws IOException {
        // Arrange
        // Simulate a realistic PKCS#7 signature result
        byte[] mockSignature = generateMockPkcs7Signature();

        when(mockNamirialSignService.pkcs7Signhash(any(InputStream.class))).thenReturn(mockSignature);

        // Act
        byte[] result;
        try (FileInputStream fis = new FileInputStream(TEST_PDF_FILE_PATH.toFile())) {
            result = service.sign(fis);
        }

        // Assert
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(mockSignature, result, "Result should be the exact bytes from NamirialSignService");

        // Verify it can be Base64 encoded (common operation after signing)
        String base64Encoded = Base64.getEncoder().encodeToString(result);
        assertNotNull(base64Encoded, "Result should be Base64 encodable");
        assertFalse(base64Encoded.isEmpty(), "Base64 encoded result should not be empty");
    }

    @Test
    void testConstructor_InjectsNamirialSignService() {
        // Arrange
        NamirialSignService customService = mock(NamirialSignService.class);
        byte[] expectedResult = "custom-result".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(TEST_PDF_CONTENT.getBytes(StandardCharsets.UTF_8));

        when(customService.pkcs7Signhash(any(InputStream.class))).thenReturn(expectedResult);

        // Act
        NamirialPkcs7HashSignServiceImpl customServiceImpl = new NamirialPkcs7HashSignServiceImpl(customService);
        byte[] result = customServiceImpl.sign(inputStream);

        // Assert
        assertArrayEquals(expectedResult, result, "Should use the injected NamirialSignService");
        verify(customService, times(1)).pkcs7Signhash(inputStream);
    }

    @Test
    void testSign_MultipleInvocations() {
        // Arrange
        byte[] result1 = "result1".getBytes(StandardCharsets.UTF_8);
        byte[] result2 = "result2".getBytes(StandardCharsets.UTF_8);

        InputStream stream1 = new ByteArrayInputStream("content1".getBytes(StandardCharsets.UTF_8));
        InputStream stream2 = new ByteArrayInputStream("content2".getBytes(StandardCharsets.UTF_8));

        when(mockNamirialSignService.pkcs7Signhash(stream1)).thenReturn(result1);
        when(mockNamirialSignService.pkcs7Signhash(stream2)).thenReturn(result2);

        // Act
        byte[] firstResult = service.sign(stream1);
        byte[] secondResult = service.sign(stream2);

        // Assert
        assertArrayEquals(result1, firstResult, "First invocation should return first result");
        assertArrayEquals(result2, secondResult, "Second invocation should return second result");

        verify(mockNamirialSignService, times(1)).pkcs7Signhash(stream1);
        verify(mockNamirialSignService, times(1)).pkcs7Signhash(stream2);
    }

    private byte[] generateMockPkcs7Signature() {
        // Create a mock signature that resembles a PKCS#7 structure
        String mockSignature = "MIAGCSqGSIb3DQEHAqCAMIACAQExDTALBglghkgBZQMEAgEwCwYJKoZIhvcNAQcBoIAwggOf";
        return Base64.getDecoder().decode(mockSignature);
    }
}

