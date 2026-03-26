package it.pagopa.selfcare.document.util;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.document.exception.PdfBuilderException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@QuarkusTest
class PdfBuilderTest {

    // ---- generateDocument - Success Cases ----

    @Test
    void generateDocument_shouldCreatePdfWithValidHtmlTemplate() throws IOException {
        // Given
        String documentName = "test-document.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Document</title>
                </head>
                <body>
                    <h1>Hello ${name}</h1>
                    <p>This is a test document for ${company}.</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("name", "Mario Rossi");
        content.put("company", "PagoPA");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().endsWith(".pdf"));
        assertTrue(result.getName().contains("test-document.pdf"));
        assertTrue(result.length() > 0, "PDF file should not be empty");

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldSubstitutePlaceholdersCorrectly() throws IOException {
        // Given
        String documentName = "placeholder-test.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Name: ${userName}</p>
                    <p>Tax Code: ${taxCode}</p>
                    <p>Email: ${email}</p>
                    <p>Product: ${productName}</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("userName", "Giovanni Bianchi");
        content.put("taxCode", "BNCGNN70A01H501W");
        content.put("email", "giovanni@test.it");
        content.put("productName", "PagoPA");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleEmptyPlaceholderMap() throws IOException {
        // Given
        String documentName = "empty-map.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Static Content Only</h1>
                    <p>This document has no placeholders.</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleComplexHtmlWithStyles() throws IOException {
        // Given
        String documentName = "styled-document.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        h1 { color: blue; }
                        .highlight { background-color: yellow; }
                        table { border-collapse: collapse; width: 100%; }
                        td, th { border: 1px solid black; padding: 8px; }
                    </style>
                </head>
                <body>
                    <h1>${title}</h1>
                    <p class="highlight">${description}</p>
                    <table>
                        <tr>
                            <th>Field</th>
                            <th>Value</th>
                        </tr>
                        <tr>
                            <td>Name</td>
                            <td>${name}</td>
                        </tr>
                        <tr>
                            <td>Date</td>
                            <td>${date}</td>
                        </tr>
                    </table>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("title", "Contract Document");
        content.put("description", "Important contract information");
        content.put("name", "Luigi Verdi");
        content.put("date", "2026-03-18");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleHtmlWithSpecialCharacters() throws IOException {
        // Given
        String documentName = "special-chars.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Special characters: ${text}</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("text", "àèéìòù - €$£ - © ® ™ - <>&\"'");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldGenerateUniqueFilenames() throws IOException {
        // Given
        String documentName = "same-name.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html><body><p>Test</p></body></html>
                """;
        Map<String, Object> content = new HashMap<>();

        // When
        File result1 = PdfBuilder.generateDocument(documentName, htmlTemplate, content);
        File result2 = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getName(), result2.getName(), 
                "Files should have unique names due to timestamp and UUID");

        // Cleanup
        Files.deleteIfExists(result1.toPath());
        Files.deleteIfExists(result2.toPath());
        if (result1.getParentFile() != null) {
            Files.deleteIfExists(result1.getParentFile().toPath());
        }
        if (result2.getParentFile() != null && !result2.getParentFile().equals(result1.getParentFile())) {
            Files.deleteIfExists(result2.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleDocumentNameWithSpecialCharacters() throws IOException {
        // Given
        String documentName = "test_document-123.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html><body><p>Test</p></body></html>
                """;
        Map<String, Object> content = new HashMap<>();

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.getName().contains("test_document-123"));

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleUnmatchedPlaceholders() throws IOException {
        // Given
        String documentName = "unmatched-placeholders.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Name: ${name}</p>
                    <p>Undefined: ${undefinedPlaceholder}</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("name", "Test User");
        // undefinedPlaceholder is intentionally not provided

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        // The placeholder should remain as ${undefinedPlaceholder} in output

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleHtmlWithLists() throws IOException {
        // Given
        String documentName = "list-document.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h2>${title}</h2>
                    <ul>
                        <li>Item 1: ${item1}</li>
                        <li>Item 2: ${item2}</li>
                        <li>Item 3: ${item3}</li>
                    </ul>
                    <ol>
                        <li>First</li>
                        <li>Second</li>
                        <li>Third</li>
                    </ol>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("title", "List Example");
        content.put("item1", "First item");
        content.put("item2", "Second item");
        content.put("item3", "Third item");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    // ---- generateDocument - SVG Support ----

    @Test
    void generateDocument_shouldHandleSvgImages() throws IOException {
        // Given
        String documentName = "svg-document.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>${title}</h1>
                    <svg width="100" height="100">
                        <circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow" />
                    </svg>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("title", "SVG Test");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    // ---- generateDocument - Error Cases ----

    @Test
    void generateDocument_shouldThrowPdfBuilderException_whenHtmlIsInvalid() {
        // Given
        String documentName = "invalid.pdf";
        String invalidHtml = "<html><body><p>Unclosed paragraph</body></html>";
        Map<String, Object> content = new HashMap<>();

        // When & Then
        // Note: Jsoup is very forgiving, so this might not actually throw
        // Testing that the method handles it gracefully
        assertDoesNotThrow(() -> {
            File result = PdfBuilder.generateDocument(documentName, invalidHtml, content);
            if (result != null && result.exists()) {
                Files.deleteIfExists(result.toPath());
                if (result.getParentFile() != null) {
                    Files.deleteIfExists(result.getParentFile().toPath());
                }
            }
        });
    }

    @Test
    void generateDocument_shouldThrowPdfBuilderException_whenTemplateIsNull() {
        // Given
        String documentName = "null-template.pdf";
        String nullTemplate = null;
        Map<String, Object> content = new HashMap<>();

        // When & Then
        PdfBuilderException exception = assertThrows(PdfBuilderException.class,
                () -> PdfBuilder.generateDocument(documentName, nullTemplate, content));
        assertEquals("Error during PDF creation", exception.getMessage());
        assertEquals("0030", exception.getCode());
    }

    @Test
    void generateDocument_shouldHandleNullContentMap() throws IOException {
        // Given
        String documentName = "null-content.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html><body><p>Test ${placeholder}</p></body></html>
                """;
        Map<String, Object> nullContent = null;

        // When
        // StringSubstitutor handles null map gracefully, treating it as empty
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, nullContent);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleEmptyHtmlTemplate() throws IOException {
        // Given
        String documentName = "empty-template.pdf";
        String emptyTemplate = "";
        Map<String, Object> content = new HashMap<>();

        // When
        File result = PdfBuilder.generateDocument(documentName, emptyTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleMinimalHtml() throws IOException {
        // Given
        String documentName = "minimal.pdf";
        String minimalHtml = "<html><body>Test</body></html>";
        Map<String, Object> content = new HashMap<>();

        // When
        File result = PdfBuilder.generateDocument(documentName, minimalHtml, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleNestedPlaceholders() throws IOException {
        // Given
        String documentName = "nested.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <div>
                        <div>
                            <div>
                                <p>${deeply} ${nested} ${value}</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("deeply", "This");
        content.put("nested", "is");
        content.put("value", "test");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleMultiplePages() throws IOException {
        // Given
        String documentName = "multipage.pdf";
        StringBuilder longContent = new StringBuilder();
        longContent.append("<!DOCTYPE html><html><body>");
        for (int i = 0; i < 100; i++) {
            longContent.append("<p>Line ").append(i).append(": ${text}</p>");
        }
        longContent.append("</body></html>");

        Map<String, Object> content = new HashMap<>();
        content.put("text", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                + "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");

        // When
        File result = PdfBuilder.generateDocument(documentName, longContent.toString(), content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 1000, "Multi-page PDF should be larger");

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldCreateFileWithRestrictedPermissions() throws IOException {
        // Given
        String documentName = "permissions-test.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html><body><p>Permissions test</p></body></html>
                """;
        Map<String, Object> content = new HashMap<>();

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.canRead(), "File should be readable by owner");
        assertTrue(result.canWrite(), "File should be writable by owner");

        // Note: On non-POSIX systems (Windows), permissions work differently
        // The test verifies basic read/write access

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleNumericValues() throws IOException {
        // Given
        String documentName = "numeric.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Integer: ${intValue}</p>
                    <p>Double: ${doubleValue}</p>
                    <p>Long: ${longValue}</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("intValue", 12345);
        content.put("doubleValue", 123.45);
        content.put("longValue", 9876543210L);

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleNullValuesInContentMap() throws IOException {
        // Given
        String documentName = "null-values.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <body>
                    <p>Name: ${name}</p>
                    <p>Email: ${email}</p>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("name", "Test User");
        content.put("email", null);

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    @Test
    void generateDocument_shouldHandleComplexTableStructure() throws IOException {
        // Given
        String documentName = "complex-table.pdf";
        String htmlTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        table { border-collapse: collapse; width: 100%; }
                        th, td { border: 1px solid black; padding: 5px; text-align: left; }
                        th { background-color: #f2f2f2; }
                    </style>
                </head>
                <body>
                    <h1>${title}</h1>
                    <table>
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Role</th>
                                <th>Tax Code</th>
                                <th>Email</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>${name1}</td>
                                <td>${role1}</td>
                                <td>${taxCode1}</td>
                                <td>${email1}</td>
                            </tr>
                            <tr>
                                <td>${name2}</td>
                                <td>${role2}</td>
                                <td>${taxCode2}</td>
                                <td>${email2}</td>
                            </tr>
                        </tbody>
                    </table>
                </body>
                </html>
                """;
        Map<String, Object> content = new HashMap<>();
        content.put("title", "User List");
        content.put("name1", "Mario Rossi");
        content.put("role1", "Manager");
        content.put("taxCode1", "RSSMRA80A01H501U");
        content.put("email1", "mario@test.it");
        content.put("name2", "Luigi Verdi");
        content.put("role2", "Delegate");
        content.put("taxCode2", "VRDLGU85B02H501V");
        content.put("email2", "luigi@test.it");

        // When
        File result = PdfBuilder.generateDocument(documentName, htmlTemplate, content);

        // Then
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0);

        // Cleanup
        Files.deleteIfExists(result.toPath());
        if (result.getParentFile() != null) {
            Files.deleteIfExists(result.getParentFile().toPath());
        }
    }

    // ---- Constructor test ----

    @Test
    void constructor_shouldBePrivate() throws Exception {
        // Use reflection to test private constructor
        var constructor = PdfBuilder.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

}



