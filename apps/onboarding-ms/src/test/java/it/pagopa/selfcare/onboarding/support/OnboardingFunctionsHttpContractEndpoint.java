package it.pagopa.selfcare.onboarding.support;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.service.OrchestrationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Test-only bridge exercising the real request context and generated onboarding-functions REST client. */
@Path("/onboarding-functions-http-contract")
@ApplicationScoped
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class OnboardingFunctionsHttpContractEndpoint {
    @Inject OrchestrationService orchestrationService;

    @GET
    @Path("/start/{onboardingId}")
    public Uni<Response> start(@PathParam("onboardingId") String onboardingId) {
        return orchestrationService.triggerOrchestrationIfEnabled(onboardingId, null)
                .map(response -> Response.ok(response).build());
    }

    @GET
    @Path("/delete/{onboardingId}")
    public Uni<Response> delete(@PathParam("onboardingId") String onboardingId) {
        return orchestrationService.triggerOrchestrationDeleteInstitutionAndUser(onboardingId)
                .map(response -> Response.ok(response).build());
    }
}
