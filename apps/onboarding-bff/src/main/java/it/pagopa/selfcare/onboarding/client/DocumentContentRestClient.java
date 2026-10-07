package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.transport.ReplayOnConnectionDrop;
import it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;

/**
 * Document content operations that need the raw downstream response: the generated client exposes downloads
 * as temporary files (losing the {@code Content-Disposition} file name) and fixes the file name of uploads,
 * which document-ms uses to recognise signed (p7m) attachments.
 */
@Path("/v1/document-content")
@RegisterRestClient(configKey = "document_json")
@ReplayOnConnectionDrop
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory.class)
public interface DocumentContentRestClient {

    @GET
    @Path("/{onboardingId}/contract")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getContract(@PathParam("onboardingId") String onboardingId);

    @GET
    @Path("/{onboardingId}/contract-signed")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getContractSigned(@PathParam("onboardingId") String onboardingId);

    @GET
    @Path("/{onboardingId}/template-attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getTemplateAttachment(@PathParam("onboardingId") String onboardingId,
                                   @QueryParam("institutionDescription") String institutionDescription,
                                   @QueryParam("name") String name,
                                   @QueryParam("productId") String productId,
                                   @QueryParam("templatePath") String templatePath);

    @GET
    @Path("/{onboardingId}/attachment")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getAttachment(@PathParam("onboardingId") String onboardingId,
                           @QueryParam("name") String name);

    @GET
    @Path("/aggregates-csv/{onboardingId}/products/{productId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response getAggregatesCsv(@PathParam("onboardingId") String onboardingId,
                              @PathParam("productId") String productId);

    @POST
    @Path("/upload-attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Response uploadAttachment(ClientMultipartForm form);

    @POST
    @Path("/upload-user-attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Response uploadUserAttachment(ClientMultipartForm form);
}
