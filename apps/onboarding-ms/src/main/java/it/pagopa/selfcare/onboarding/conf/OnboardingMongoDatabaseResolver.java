package it.pagopa.selfcare.onboarding.conf;

import io.quarkus.arc.Unremovable;
import io.quarkus.mongodb.panache.common.MongoDatabaseResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes every Panache operation to the database that owns the current request's product (EPIC
 * sub-task 6).
 *
 * <p>Quarkus calls this for each entity operation, so it must stay cheap and must never fail for
 * requests that carry no product context: those resolve to the shared database, which is what every
 * request does today.
 *
 * <p>Outside an active request - scheduled jobs, CDC consumers, startup - there is no request scope
 * to read, so the shared database is used. That is correct rather than a fallback: such callers are
 * not serving a product-scoped request. It is distinct from the fail-closed behaviour in {@link
 * ProductDatabaseResolver}, which applies when a product <em>is</em> known and explicitly demands a
 * dedicated database.
 */
@ApplicationScoped
@Unremovable
public class OnboardingMongoDatabaseResolver implements MongoDatabaseResolver {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OnboardingMongoDatabaseResolver.class);

  @Inject ProductRoutingContext routingContext;

  @Inject ProductDatabaseResolver productDatabaseResolver;

  @Override
  public String resolve() {
    String productId;
    try {
      productId = routingContext.getProductId();
    } catch (ContextNotActiveException e) {
      LOGGER.debug("No active request scope while resolving database, using the shared one");
      return productDatabaseResolver.sharedDatabase();
    }
    return productDatabaseResolver.resolveDatabase(productId);
  }
}
