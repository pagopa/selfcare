package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
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
