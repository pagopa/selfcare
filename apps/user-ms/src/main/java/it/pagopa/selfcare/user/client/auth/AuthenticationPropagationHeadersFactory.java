package it.pagopa.selfcare.user.client.auth;

import it.pagopa.selfcare.security.tenant.TenantConstants;
import it.pagopa.selfcare.user.conf.CurrentTenantProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Propagates the caller's identity onto outgoing service-to-service calls.
 *
 * <p>Forwarding the JWT alone is not enough once downstream services enforce tenant consistency:
 * their {@code TenantValidationFilter} rejects with 400 any request that carries a JWT but no
 * {@code X-Tenant-Id} header (Step_0 SELC-5). Every call to {@code onboarding-ms} made through this
 * factory would therefore fail, so the tenant is propagated alongside the token.
 *
 * <p>The tenant is taken from the <b>validated</b> {@code TenantContext}, never echoed from the raw
 * incoming header: the inbound filter has already reconciled that header against the JWT claim, so
 * re-reading the raw header would forward unvalidated input if the filter were ever bypassed for a
 * path.
 *
 * <p>When no tenant is resolvable - a call made outside an active request - the header is omitted,
 * mirroring how the Authorization header degrades. Adding a guessed tenant would be worse than
 * sending none.
 */
@ApplicationScoped
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String AUTHORIZATION = "Authorization";

    @Inject
    CurrentTenantProvider currentTenantProvider;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if(incomingHeaders.containsKey(AUTHORIZATION)) {
            List<String> headerValue = incomingHeaders.get(AUTHORIZATION);

            if (headerValue != null) {
                clientOutgoingHeaders.put(AUTHORIZATION, headerValue);
            }

        }

        currentTenantProvider
                .currentTenantId()
                .ifPresent(tenant ->
                        clientOutgoingHeaders.put(TenantConstants.TENANT_HEADER, List.of(tenant)));

        return clientOutgoingHeaders;
    }
}
