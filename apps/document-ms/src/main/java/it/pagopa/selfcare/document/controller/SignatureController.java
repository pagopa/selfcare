package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import it.pagopa.selfcare.document.service.SignatureService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Authenticated
@Path("/v1/signature")
@AllArgsConstructor
@Slf4j
public class SignatureController {

  @Inject
  SecurityIdentity securityIdentity;

  private final SignatureService signatureService;


}
