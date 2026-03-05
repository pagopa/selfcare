package it.pagopa.selfcare.onboarding.controller.request;

import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionBaseRequest {

    @NotNull(message = "institutionType is required")
    private InstitutionType institutionType;
    private String taxCode;
    private String subunitCode;
    private InstitutionPaSubunitType subunitType;
    @NotNull(message = "origin is required")
    private Origin origin;
    @NotEmpty(message = "originId is required")
    private String originId;
    private String city;
    private String country;
    private String county;
    private String description;
    @NotEmpty(message = "digitalAddress is required")
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String istatCode;
    private String legalForm;

    private List<GeographicTaxonomyDto> geographicTaxonomies;

    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;

    private String supportEmail;
    private String supportPhone;

    /* when onboarding is imported (es. from IO)*/
    private boolean imported;

    /* when onboarding is for PRV prod-idpay-merchant */
    private List<String> atecoCodes;
}
