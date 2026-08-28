package it.pagopa.selfcare.onboarding.client.auth;

import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Objects;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

  private static final String USER_ID_HEADER = "user-uuid";
  private static final String JWT_BEARER_TOKEN_ENV = "JWT_BEARER_TOKEN";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AuthenticationPropagationHeadersFactory.class);

  @Inject JwtSessionService tokenService;

  @Override
  public MultivaluedMap<String, String> update(
      MultivaluedMap<String, String> incomingHeaders,
      MultivaluedMap<String, String> clientOutgoingHeaders) {
    String bearerToken;
    // If user is founded on PDV, a bearer token is created starting from it
    if (!clientOutgoingHeaders.isEmpty() && clientOutgoingHeaders.containsKey(USER_ID_HEADER)) {
      final String uuid = clientOutgoingHeaders.get(USER_ID_HEADER).get(0);
      final String jwt = tokenService.createJwt(uuid);
      bearerToken = Objects.nonNull(jwt) ? jwt : System.getenv(JWT_BEARER_TOKEN_ENV);
    } else {
      bearerToken = System.getenv(JWT_BEARER_TOKEN_ENV);
    }
    clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));

    // FIXME: this should be removed on phase 2
    clientOutgoingHeaders.put(TenantContext.TENANT_HEADER, List.of(TenantContext.resolve("")));
    LOGGER.info("Propagating tenant={}", TenantContext.currentTenantOrDefault());
    /*
    FIXME: this should be enabled on phase 2
    String tenant = incomingHeaders.getFirst(TenantContext.TENANT_HEADER);
    if (tenant == null || tenant.isBlank()) {
        tenant = TenantContext.currentTenant();
    }
    if (tenant != null && !tenant.isBlank()) {
        clientOutgoingHeaders.put(
                TenantContext.TENANT_HEADER, List.of(TenantContext.resolve(tenant)));
        LOGGER.info("Propagating tenant={}", tenant);
    }*/
    return clientOutgoingHeaders;
  }
}
