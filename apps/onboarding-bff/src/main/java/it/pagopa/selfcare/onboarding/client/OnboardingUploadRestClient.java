package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.transport.ReplayOnConnectionDrop;
import it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

/**
 * Onboarding-ms file uploads. The generated client fixes the file name and content type of every part, whereas
 * onboarding-ms (like with the former Spring client) receives the name and type sent by the caller.
 */
@Path("/v1")
@RegisterRestClient(configKey = "onboarding_json")
@ReplayOnConnectionDrop
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory.class)
public interface OnboardingUploadRestClient {

    @PUT
    @Path("/onboarding/{onboardingId}/complete")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Response completeOnboardingToken(@PathParam("onboardingId") String onboardingId, ClientMultipartForm form);

    @PUT
    @Path("/onboarding/{onboardingId}/completeOnboardingUsers")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Response completeOnboardingUsers(@PathParam("onboardingId") String onboardingId, ClientMultipartForm form);

    @POST
    @Path("/aggregates/verification/{productId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    VerifyAggregateResponse verifyAggregatesCsv(@PathParam("productId") String productId, ClientMultipartForm form);
}
