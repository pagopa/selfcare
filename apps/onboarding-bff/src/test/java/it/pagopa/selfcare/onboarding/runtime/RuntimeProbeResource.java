package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.security.AuthorizationService;
import it.pagopa.selfcare.onboarding.util.SecurityIdentityUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.MDC;

/**
 * Endpoints that expose what the application derives from the request: they exist only when the
 * runtime tests enable them, so no other test sees them.
 */
@Path("/runtime-test")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@IfBuildProperty(name = RuntimeProbeResource.ENABLED_PROPERTY, stringValue = "true")
public class RuntimeProbeResource {

    public static final String ENABLED_PROPERTY = "selfcare.runtime-test.enabled";

    @Inject
    SecurityIdentity identity;

    @Inject
    AuthorizationService authorizationService;

    @Inject
    @RestClient
    PartyRegistryProxyRestClient registry;

    @GET
    @Path("/registry")
    public Map<String, Object> registry() {
        String previous = MDC.get("traceId");
        MDC.put("traceId", "runtime-probe-trace");
        try {
            return Map.of("id", registry.getInstitutionById("inst1").getId());
        } finally {
            if (previous == null) {
                MDC.remove("traceId");
            } else {
                MDC.put("traceId", previous);
            }
        }
    }

    @GET
    @Path("/identity")
    public Map<String, Object> identity() {
        return Map.of("uid", String.valueOf(SecurityIdentityUtils.getUid(identity)));
    }

    @GET
    @Path("/permissions/{onboardingId}")
    public Map<String, Object> permission(@PathParam("onboardingId") String onboardingId,
                                          @QueryParam("permission") String permission) {
        return Map.of("granted", authorizationService.hasPermission(identity, onboardingId, permission));
    }
}
