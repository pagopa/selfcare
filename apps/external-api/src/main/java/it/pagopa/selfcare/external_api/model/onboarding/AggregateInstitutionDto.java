package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@SuppressWarnings("java:S1068")
public class AggregateInstitutionDto {
    @NotNull(message = "taxCode is required")
    private String taxCode;

    @NotNull(message = "description is required")
    private String description;

    private String subunitCode;
    private String subunitType;
    private String vatNumber;
    private String parentDescription;
    private List<GeographicTaxonomyDto> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private List<User> users;

    private String recipientCode;
    private String digitalAddress;
    private String city;
    private String county;
    private String iban;
    private String istatCode;

}
