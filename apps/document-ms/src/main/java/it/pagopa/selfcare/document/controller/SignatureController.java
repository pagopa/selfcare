package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.document.model.dto.request.SignatureRequest;
import it.pagopa.selfcare.document.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Path("/v1/signature")
@AllArgsConstructor
@Slf4j
@Tag(name = "Signature Controller", description = "Endpoints for verifying contract signatures")
public class SignatureController {

  @Inject
  SecurityIdentity securityIdentity;

  private final SignatureService signatureService;


  @Operation(summary = "Verify the signature of a contract")
  @POST
  @Path("/verify")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Void> verifyContractSignature(@Valid SignatureRequest request) {
    return signatureService.verifyContractSignature(
            request.getOnboardingId(),
            request.getFile(),
            request.getFiscalCodes(),
            request.getSkipSignatureVerification()
    );
  }

}
