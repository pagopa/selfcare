package it.pagopa.selfcare.onboarding.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.conf.CurrentTenantProvider;
import it.pagopa.selfcare.onboarding.conf.ProductRoutingContext;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.Product;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Covers the two things stamped on every onboarding before it is written: the tenant that owns it
 * and the product whose database the request must be routed to.
 */
class OnboardingPersistenceHelperTenantTest {

  @Mock CurrentTenantProvider currentTenantProvider;

  @Mock ProductRoutingContext productRoutingContext;

  @InjectMocks OnboardingPersistenceHelper helper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void stampsTheTenantOfTheCurrentRequest() {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.of("PNPG"));
    Onboarding onboarding = new Onboarding();

    helper.stampTenant(onboarding);

    assertEquals("PNPG", onboarding.getTenantId());
  }

  @Test
  void neverReassignsAnOnboardingThatAlreadyHasATenant() {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.of("AR"));
    Onboarding onboarding = new Onboarding();
    onboarding.setTenantId("PNPG");

    helper.stampTenant(onboarding);

    assertEquals("PNPG", onboarding.getTenantId());
  }

  @Test
  void leavesTheOnboardingUntaggedWhenNoTenantIsResolvable() {
    when(currentTenantProvider.currentTenantId()).thenReturn(Optional.empty());
    Onboarding onboarding = new Onboarding();

    helper.stampTenant(onboarding);

    assertNull(onboarding.getTenantId());
  }

  @Test
  void routesTheRequestToTheProductDatabase() {
    Product product = new Product();
    product.setId("prod-io");

    helper.routeToProductDatabase(product);

    verify(productRoutingContext).setProductId("prod-io");
  }

  @Test
  void doesNotRouteWhenTheProductIsUnusable() {
    helper.routeToProductDatabase(null);
    helper.routeToProductDatabase(new Product());

    verify(productRoutingContext, never()).setProductId(org.mockito.ArgumentMatchers.anyString());
  }
}
