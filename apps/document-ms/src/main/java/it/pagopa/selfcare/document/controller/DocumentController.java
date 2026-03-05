package it.pagopa.selfcare.document.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Authenticated
@Path("/v1/documents")
@AllArgsConstructor
@Slf4j
public class DocumentController {

  @Inject
  SecurityIdentity securityIdentity;
}
