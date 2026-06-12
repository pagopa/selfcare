package it.pagopa.selfcare.external_api.model.institution;

import it.pagopa.selfcare.external_api.model.onboarding.GeographicTaxonomyDto;
import it.pagopa.selfcare.external_api.model.onboarding.User;
import it.pagopa.selfcare.onboarding.common.Origin;
import lombok.Data;

import java.util.List;

@Data
public class AggregateInsitution {

    private String taxCode;
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
