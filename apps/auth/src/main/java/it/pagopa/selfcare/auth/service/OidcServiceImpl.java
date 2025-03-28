package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OidcServiceImpl implements OidcService {


    @Override
    public Uni<OidcExchangeResponse> exchange(String authCode, String redirectUri) {
        return Uni.createFrom().item(OidcExchangeResponse.builder().sessionToken("").build());
    }
}
