package it.pagopa.selfcare.security;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * Enriches the logging context (MDC) of every REST request with a configurable set of claims taken
 * from the caller's JWT, so that every log line written while serving the request carries them.
 *
 * <p>The claims to copy are configured with {@code selfcare.logging.mdc.claims} (comma separated,
 * default {@code uid}); each claim is stored in the MDC under its own name, so it can be printed by
 * adding e.g. {@code %X{uid}} to {@code quarkus.log.console.format}. The JBoss LogManager MDC is
 * used directly because it is the one read by the Quarkus log formatter (it is also the backend of
 * {@code org.slf4j.MDC} in Quarkus applications).
 *
 * <p>The identity is resolved through the deferred identity, so the filter works both with
 * proactive and with lazy ({@code quarkus.http.auth.proactive=false}) authentication without
 * blocking the event loop. The filter never aborts a request.
 */
public class LoggingContextFilter {

  private static final Logger LOG = Logger.getLogger(LoggingContextFilter.class);
  private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");
  private static final Pattern NON_LOG_SAFE_CHARS = Pattern.compile("[^\\p{Print}]");
  private static final int MAX_LOG_VALUE_LENGTH = 512;

  @Inject CurrentIdentityAssociation identityAssociation;

  @ConfigProperty(name = "selfcare.logging.mdc.claims", defaultValue = "uid")
  List<String> claims;

  @ServerRequestFilter(priority = Priorities.AUTHORIZATION + 1)
  public Uni<Void> addClaimsToLoggingContext(ContainerRequestContext requestContext) {
    clearLoggingContext();
    String path = requestContext.getUriInfo().getPath();
    if (isExcluded(path)) {
      return Uni.createFrom().voidItem();
    }
    String method = requestContext.getMethod();
    return identityAssociation
        .getDeferredIdentity()
        .onFailure()
        .recoverWithNull()
        .invoke(identity -> populateLoggingContext(identity, method, path))
        .replaceWithVoid();
  }

  @ServerResponseFilter
  public void removeClaimsFromLoggingContext() {
    clearLoggingContext();
  }

  void populateLoggingContext(SecurityIdentity identity, String method, String path) {
    if (identity == null
        || identity.isAnonymous()
        || !(identity.getPrincipal() instanceof JsonWebToken jwt)) {
      LOG.debugf(
          "No JWT in request %s %s, claims %s not available in logging context",
          sanitize(method), sanitize(path), claims);
      return;
    }
    for (String claim : claims) {
      String value = claimValue(jwt, claim);
      if (value == null) {
        LOG.warnf(
            "Claim '%s' not found in JWT for request %s %s",
            claim, sanitize(method), sanitize(path));
      } else {
        MDC.put(claim, value);
      }
    }
  }

  private void clearLoggingContext() {
    claims.forEach(MDC::remove);
  }

  private static String claimValue(JsonWebToken jwt, String claim) {
    Object value = jwt.getClaim(claim);
    if (value == null) {
      return null;
    }
    String stringValue =
        value instanceof JsonString jsonString ? jsonString.getString() : String.valueOf(value);
    return stringValue.isBlank() ? null : sanitize(stringValue);
  }

  private static boolean isExcluded(String path) {
    if (path == null) {
      return false;
    }
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return normalized.equals("q") || normalized.startsWith("q/");
  }

  /** Prevents log forging by neutralizing unsafe characters from user-controlled values. */
  private static String sanitize(String value) {
    if (value == null) {
      return null;
    }
    String sanitized = CONTROL_CHARS.matcher(value).replaceAll("_");
    sanitized = NON_LOG_SAFE_CHARS.matcher(sanitized).replaceAll("_");
    return sanitized.length() > MAX_LOG_VALUE_LENGTH
        ? sanitized.substring(0, MAX_LOG_VALUE_LENGTH)
        : sanitized;
  }
}

