package it.pagopa.selfcare.dashboard.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class FilterAuthorityDomain {

    private final String institutionId;
    private final String productId;
    private final String groupId;

}
