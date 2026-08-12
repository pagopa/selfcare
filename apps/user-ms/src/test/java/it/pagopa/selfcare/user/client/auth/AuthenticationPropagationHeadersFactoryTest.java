package it.pagopa.selfcare.user.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.user.conf.CurrentTenantProvider;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuthenticationPropagationHeadersFactoryTest {

  @Mock CurrentTenantProvider currentTenantProvider;

  @InjectMocks AuthenticationPropagationHeadersFactory factory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void propagatesAuthorizationAndTenant() {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.of("AR"));
    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    incoming.put("Authorization", List.of("Bearer token"));

    MultivaluedMap<String, String> result = factory.update(incoming, new MultivaluedHashMap<>());

    assertEquals("Bearer token", result.getFirst("Authorization"));
    assertEquals("AR", result.getFirst("X-Tenant-Id"));
  }

  @Test
  void omitsTenantHeaderOutsideAnActiveRequest() {
    // Scheduled/event-driven code has no validated tenant: a guessed value would be worse than none.
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.empty());

    MultivaluedMap<String, String> result =
        factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());

    assertFalse(result.containsKey("X-Tenant-Id"));
  }

  @Test
  void doesNotEchoTheRawIncomingTenantHeader() {
    // The header must come from the validated context, never from unvalidated inbound input.
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.empty());
    MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
    incoming.put("X-Tenant-Id", List.of("PNPG"));

    MultivaluedMap<String, String> result = factory.update(incoming, new MultivaluedHashMap<>());

    assertFalse(result.containsKey("X-Tenant-Id"));
  }
}
