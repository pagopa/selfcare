package it.pagopa.selfcare.external_api.security.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces tenant consistency on every authenticated request (Step_0 SELC-1, SELC-2, SELC-3),
 * mirroring {@code it.pagopa.selfcare.security.tenant.TenantValidationFilter} from the Quarkus
 * {@code selfcare-sdk-security} library for this Spring Boot service:
 *
 * <ul>
 *   <li>rejects requests with a missing, duplicated, or unknown {@code X-Tenant-Id} header
 *       (SELC-1.3) — no silent default is ever applied to the header;</li>
 *   <li>reconciles the header with the JWT {@code tenant_id} claim, rejecting on mismatch
 *       (SELC-2.3);</li>
 *   <li>defaults the claim to {@code PNPG} only for hub-spid-login-issued tokens missing it
 *       (SELC-3.1), while still requiring the header to be present and equal to {@code PNPG}
 *       (SELC-3.4);</li>
 *   <li>exposes the validated tenant as the {@value TenantConstants#TENANT_REQUEST_ATTRIBUTE}
 *       request attribute for downstream code.</li>
 * </ul>
 *
 * <p>Runs as a plain servlet {@link jakarta.servlet.Filter} registered outside the Spring Security
 * filter chain (which Spring Boot wires in at a very low order); by default such a bean is invoked
 * after the security chain, so {@link SecurityContextHolder} is already populated when this filter
 * runs. Requests without an authenticated {@link SelfCareUser} principal (e.g. public/health
 * endpoints) are never rejected here, following SELC-2.2 ("for the lifetime of the authenticated
 * session"); they are still tenant-scoped from a usable {@code X-Tenant-Id} header when one is
 * present, so that subscription-key authenticated server-to-server calls do not run unscoped across
 * all tenants. The JWT is not re-verified here: it has already been cryptographically validated by
 * the upstream {@code JwtAuthenticationFilter}/{@code JwtAuthenticationProvider} (selc-commons-web)
 * that populated the {@link Authentication}; this filter only decodes the already-trusted payload
 * to read the {@code tenant_id} claim.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class TenantValidationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantValidationFilter.class);

  private final ObjectMapper objectMapper;

  public TenantValidationFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String issuer = resolveIssuer(authentication);
    if (issuer == null || issuer.isBlank()) {
      // No authenticated session, so there is no tenant claim to reconcile against and this filter
      // must not reject: public and health endpoints legitimately land here.
      //
      // But "no JWT" is NOT the same as "no tenant". Server-to-server callers authenticated with an
      // Ocp-Apim-Subscription-Key rather than a JWT also land here, and leaving the tenant unset
      // makes every downstream repository query run UNSCOPED across all tenants. So when such a
      // request carries a usable X-Tenant-Id we honour it and scope the request to it.
      //
      // Trusting the header is safe only because it is not caller-controlled: the APIM inbound
      // policy overrides X-Tenant-Id unconditionally on every request it forwards, deriving it from
      // the caller's origin or, for s2s callers, from their subscription id (see
      // infra/resources/_modules/apim_api). A request that reaches a service without passing
      // through APIM cannot reach it from outside the private network either.
      resolveHeaderTenant(request)
          .ifPresent(t -> request.setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, t));
      filterChain.doFilter(request, response);
      return;
    }

    Optional<TenantId> headerTenant = resolveHeaderTenant(request);
    if (headerTenant.isEmpty()) {
      reject(response, "Missing, duplicated, or unknown X-Tenant-Id header");
      return;
    }

    Optional<TenantId> claimTenant = resolveClaimTenant(authentication, issuer);
    if (claimTenant.isEmpty()) {
      reject(response, "Missing or unknown tenant_id claim in JWT");
      return;
    }

    if (headerTenant.get() != claimTenant.get()) {
      LOGGER.warn(
          "Tenant mismatch rejected: header tenant={}, claim tenant={}, issuer={}",
          headerTenant.get(), claimTenant.get(), issuer);
      reject(response, "X-Tenant-Id header does not match the JWT tenant claim");
      return;
    }

    request.setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, headerTenant.get());
    filterChain.doFilter(request, response);
  }

  private String resolveIssuer(Authentication authentication) {
    if (authentication == null
        || !(authentication.getPrincipal() instanceof SelfCareUser selfCareUser)) {
      return null;
    }
    return selfCareUser.getIssuer();
  }

  private Optional<TenantId> resolveHeaderTenant(HttpServletRequest request) {
    Enumeration<String> headers = request.getHeaders(TenantConstants.TENANT_HEADER);
    List<String> values = headers == null ? List.of() : Collections.list(headers);
    if (values.size() != 1) {
      if (!values.isEmpty()) {
        // Multiple X-Tenant-Id header values on the same request: treat as invalid/spoofed
        // input, never pick one silently.
        LOGGER.warn("Rejected request carrying multiple X-Tenant-Id header values");
      }
      return Optional.empty();
    }
    return TenantId.fromValue(values.get(0));
  }

  private Optional<TenantId> resolveClaimTenant(Authentication authentication, String issuer) {
    String claimValue = extractTenantClaim(authentication);
    if (claimValue == null || claimValue.isBlank()) {
      if (TenantConstants.HUB_SPID_LOGIN_ISSUER.equals(issuer)) {
        LOGGER.info(
            "hub-spid-login token missing tenant_id claim; defaulting to {} (Step_0 SELC-3.1)",
            TenantConstants.HUB_SPID_LOGIN_DEFAULT_TENANT);
        return Optional.of(TenantConstants.HUB_SPID_LOGIN_DEFAULT_TENANT);
      }
      return Optional.empty();
    }
    return TenantId.fromValue(claimValue);
  }

  private String extractTenantClaim(Authentication authentication) {
    if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
      return null;
    }
    String[] parts = token.split("\\.");
    if (parts.length < 2) {
      return null;
    }
    try {
      byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64Url(parts[1]));
      Map<String, Object> claims =
          objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
      Object claim = claims.get(TenantConstants.TENANT_CLAIM);
      return claim == null ? null : claim.toString();
    } catch (Exception e) {
      LOGGER.warn("Unable to decode JWT payload while resolving tenant claim", e);
      return null;
    }
  }

  private static String padBase64Url(String value) {
    int padding = (4 - value.length() % 4) % 4;
    return value + "=".repeat(padding);
  }

  private void reject(HttpServletResponse response, String detail) throws IOException {
    LOGGER.warn("Tenant validation rejected request: {}", detail);
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("application/problem+json");
    objectMapper.writeValue(response.getWriter(), new TenantProblem(400, "Invalid tenant", detail));
  }
}
