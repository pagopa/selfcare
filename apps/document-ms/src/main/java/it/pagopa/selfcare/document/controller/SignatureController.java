package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;

@Authenticated
@Path("/v1/signature")
@AllArgsConstructor
@Slf4j
public class SignatureController {

  @Inject
  SecurityIdentity securityIdentity;

  private final SignatureService signatureService;

  //@Operation(summary = "Sign a document", description = "Signs a PDF document and returns the signed version.")
  @POST
  @Path("/sign")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<RestResponse<File>> sign(
          @NotNull @RestForm("file") File file,
          @NotNull @QueryParam("institutionDescription") String institutionDescription,
          @NotNull @QueryParam("productId") String productId
  ) {
      return signatureService.signDocument(file, institutionDescription, productId);
  }

}
