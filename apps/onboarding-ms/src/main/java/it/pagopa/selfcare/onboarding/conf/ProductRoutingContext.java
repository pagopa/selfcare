package it.pagopa.selfcare.onboarding.conf;

import jakarta.enterprise.context.RequestScoped;

/**
 * Carries the product whose database the current request must be routed to.
 *
 * <p>Quarkus' {@code MongoDatabaseResolver#resolve()} takes no arguments, so the product cannot be
 * passed down the call chain to it - it has to be read from the request. This holder is that
 * channel, populated once per request and read by {@link OnboardingMongoDatabaseResolver}.
 *
 * <p>It is intentionally empty by default: an unset product resolves to the shared database, which
 * is the behaviour every request has today.
 */
@RequestScoped
public class ProductRoutingContext {

  private String productId;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public void clear() {
    this.productId = null;
  }
}
