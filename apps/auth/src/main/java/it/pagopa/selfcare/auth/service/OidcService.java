package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;


public interface OidcService {
    Uni<OidcExchangeResponse> exchange(String authCode, String redirectUri) throws ForbiddenException, ResourceNotFoundException, InternalException;
}
