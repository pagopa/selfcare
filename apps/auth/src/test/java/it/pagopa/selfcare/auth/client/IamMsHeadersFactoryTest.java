package it.pagopa.selfcare.auth.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.context.TokenContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class IamMsHeadersFactoryTest {

  @Mock TokenContext tokenContext;

  @InjectMocks IamMsHeadersFactory headersFactory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testUpdate_WithToken() {
    // Arrange
    String testToken = "test-token-123";
    when(tokenContext.getToken()).thenReturn(testToken);

    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
    outgoing.add("Content-Type", "application/json");

    // Act
    MultivaluedMap<String, String> result = headersFactory.update(incoming, outgoing);

    // Assert
    assertNotNull(result);
    assertEquals("application/json", result.getFirst("Content-Type"));
    assertEquals("Bearer " + testToken, result.getFirst("Authorization"));
  }

  @Test
  void testUpdate_WithoutToken() {
    // Arrange
    when(tokenContext.getToken()).thenReturn(null);

    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
    outgoing.add("Content-Type", "application/json");

    // Act
    MultivaluedMap<String, String> result = headersFactory.update(incoming, outgoing);

    // Assert
    assertNotNull(result);
    assertEquals("application/json", result.getFirst("Content-Type"));
    assertFalse(result.containsKey("Authorization"));
    verify(tokenContext).getToken();
  }

  @Test
  void testUpdate_PreservesOutgoingHeaders() {
    // Arrange
    String testToken = "test-token-456";
    when(tokenContext.getToken()).thenReturn(testToken);

    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();
    outgoing.add("Content-Type", "application/json");
    outgoing.add("Accept", "application/json");
    outgoing.add("Custom-Header", "custom-value");

    // Act
    MultivaluedMap<String, String> result = headersFactory.update(incoming, outgoing);

    // Assert
    assertNotNull(result);
    assertEquals(4, result.size());
    assertEquals("application/json", result.getFirst("Content-Type"));
    assertEquals("application/json", result.getFirst("Accept"));
    assertEquals("custom-value", result.getFirst("Custom-Header"));
    assertEquals("Bearer " + testToken, result.getFirst("Authorization"));
  }

  @Test
  void testUpdate_EmptyOutgoingHeaders() {
    // Arrange
    String testToken = "test-token-789";
    when(tokenContext.getToken()).thenReturn(testToken);

    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    MultivaluedMap<String, String> outgoing = new MultivaluedHashMap<>();

    // Act
    MultivaluedMap<String, String> result = headersFactory.update(incoming, outgoing);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Bearer " + testToken, result.getFirst("Authorization"));
  }
}
