package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PDNDTokenForm {
    @feign.form.FormProperty("client_assertion") String clientAssertion;
    @feign.form.FormProperty("client_assertion_type") String clientAssertionType;
    @feign.form.FormProperty("grant_type") String grantType;
    @feign.form.FormProperty("client_id") String clientId;
}
