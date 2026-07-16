package it.pagopa.selfcare.onboarding.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/onboarding")
@RegisterRestClient(configKey = "onboarding_json")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory.class)
public interface OnboardingWorkflowRestClient {

    @PUT
    @Path("/{onboardingId}/approve")
    @Consumes(APPLICATION_JSON)
    Response approve(@PathParam("onboardingId") String onboardingId, Map<String, String> request);

    @PUT
    @Path("/{onboardingId}/reject")
    @Consumes(APPLICATION_JSON)
    Response reject(@PathParam("onboardingId") String onboardingId, Map<String, String> request);

    @PUT
    @Path("/{onboardingId}")
    Response triggerDocumentGate(@PathParam("onboardingId") String onboardingId);
}
