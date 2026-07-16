package it.pagopa.selfcare.onboarding.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ms-iam")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory.class)
public interface IamRestClient {
    @GET
    @Path("/iam/users/{userId}/permissions/{permission}")
    Response hasIAMUserPermission(@PathParam("permission") String permission,
                                  @PathParam("userId") String userId,
                                  @QueryParam("institutionId") String institutionId,
                                  @QueryParam("productId") String productId);
}
