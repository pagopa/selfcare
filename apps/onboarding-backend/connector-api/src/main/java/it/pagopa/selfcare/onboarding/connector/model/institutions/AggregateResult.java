package it.pagopa.selfcare.onboarding.connector.model.institutions;

import lombok.Data;

import java.util.List;

@Data
public class AggregateResult {
    private String subunitCode;
    private String subunitType;
    private String description;
    private String recipientCode;
    private String digitalAddress;
    private String taxCode;
    private String vatNumber;
    private String address;
    private String city;
    private String county;
    private String zipCode;
    private String originId;
    private String origin;
    private String iban;
    private String parentDescription;
    private List<AggregateUserResult> users = null;
}
