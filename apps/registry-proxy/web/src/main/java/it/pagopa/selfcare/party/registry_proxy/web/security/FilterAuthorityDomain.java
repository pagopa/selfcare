package it.pagopa.selfcare.party.registry_proxy.web.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class FilterAuthorityDomain {

    private final String issuer;

}
