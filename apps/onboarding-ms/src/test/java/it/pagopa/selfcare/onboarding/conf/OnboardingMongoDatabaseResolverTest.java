package it.pagopa.selfcare.onboarding.conf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.exception.UnresolvableProductDatabaseException;
import jakarta.enterprise.context.ContextNotActiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Covers the two behaviours that make this resolver safe to enable on every Panache operation: it
 * must never throw for callers that legitimately have no request scope, and it must not swallow a
 * genuine misconfiguration.
 */
class OnboardingMongoDatabaseResolverTest {

  private static final String SHARED = "selcOnboarding";

  @Mock ProductRoutingContext routingContext;

  @Mock ProductDatabaseResolver productDatabaseResolver;

  @InjectMocks OnboardingMongoDatabaseResolver resolver;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void resolvesTheDatabaseOfTheRequestProduct() {
    when(routingContext.getProductId()).thenReturn("prod-dedicated");
    when(productDatabaseResolver.resolveDatabase("prod-dedicated")).thenReturn("dedicatedDb");

    assertEquals("dedicatedDb", resolver.resolve());
  }

  @Test
  void outsideAnActiveRequestFallsBackToTheSharedDatabase() {
    doThrow(new ContextNotActiveException()).when(routingContext).getProductId();
    when(productDatabaseResolver.sharedDatabase()).thenReturn(SHARED);

    assertEquals(SHARED, resolver.resolve());
  }

  @Test
  void requestWithoutProductContextResolvesToShared() {
    when(routingContext.getProductId()).thenReturn(null);
    when(productDatabaseResolver.resolveDatabase(null)).thenReturn(SHARED);

    assertEquals(SHARED, resolver.resolve());
  }

  @Test
  void misconfiguredProductStillFailsClosed() {
    when(routingContext.getProductId()).thenReturn("prod-broken");
    when(productDatabaseResolver.resolveDatabase("prod-broken"))
        .thenThrow(new UnresolvableProductDatabaseException("no databaseName"));

    assertThrows(UnresolvableProductDatabaseException.class, () -> resolver.resolve());
  }
}
