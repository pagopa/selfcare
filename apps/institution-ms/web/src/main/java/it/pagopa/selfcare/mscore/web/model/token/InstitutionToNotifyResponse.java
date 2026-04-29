package it.pagopa.selfcare.mscore.web.model.token;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.mscore.model.RootParent;
import it.pagopa.selfcare.mscore.model.institution.PaymentServiceProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstitutionToNotifyResponse {

    private InstitutionType institutionType;
    private String description;
    private String digitalAddress;
    private String address;
    private String taxCode;
    private String origin;
    private String originId;
    private String zipCode;
    private PaymentServiceProvider paymentServiceProvider;
    private String istatCode;
    private String city;
    private String country;
    private String county;
    private String subUnitCode;
    private String category;
    private String subUnitType;
    private RootParent rootParent;

}
