package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.institution.*;
import it.pagopa.selfcare.external_api.model.onboarding.DataProtectionOfficer;
import it.pagopa.selfcare.external_api.model.onboarding.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.NONE)
public class InstitutionMapperCustom {

    public static InstitutionUpdateResponse toInstitutionUpdateResponse(Institution institution) {
        InstitutionUpdateResponse institutionUpdate = new InstitutionUpdateResponse();
        institutionUpdate.setAddress(institution.getAddress());
        institutionUpdate.setInstitutionType(Optional.ofNullable(institution.getInstitutionType()).map(InstitutionType::valueOf).orElse(null));
        institutionUpdate.setDescription(institution.getDescription());
        institutionUpdate.setDigitalAddress(institution.getDigitalAddress());
        institutionUpdate.setTaxCode(institution.getTaxCode());
        institutionUpdate.setZipCode(institution.getZipCode());
        institutionUpdate.setPaymentServiceProvider(toPaymentServiceProviderResponse(institution.getPaymentServiceProvider()));
        institutionUpdate.setDataProtectionOfficer(toDataProtectionOfficerResponse(institution.getDataProtectionOfficer()));
        if (institution.getGeographicTaxonomies() != null) {
            var geoCodes = institution.getGeographicTaxonomies().stream()
                    .map(GeographicTaxonomy::getCode)
                    .toList();
            institutionUpdate.setGeographicTaxonomies(geoCodes);
        }
        institutionUpdate.setRea(institution.getRea());
        institutionUpdate.setShareCapital(institution.getShareCapital());
        institutionUpdate.setBusinessRegisterPlace(institution.getBusinessRegisterPlace());
        institutionUpdate.setSupportEmail(institution.getSupportEmail());
        institutionUpdate.setSupportPhone(institution.getSupportPhone());
        institutionUpdate.setImported(institution.isImported());

        return institutionUpdate;
    }

    public static BillingResponse toBillingResponse(Onboarding onboarding, Institution institution) {
        BillingResponse billingResponse = new BillingResponse();
        if (onboarding.getBilling() != null) {
            billingResponse.setVatNumber(onboarding.getBilling().getVatNumber());
            billingResponse.setRecipientCode(onboarding.getBilling().getRecipientCode());
            billingResponse.setPublicServices(onboarding.getBilling().getPublicServices());
        } else if (institution.getBilling() != null) {
            billingResponse.setVatNumber(institution.getBilling().getVatNumber());
            billingResponse.setRecipientCode(institution.getBilling().getRecipientCode());
            billingResponse.setPublicServices(institution.getBilling().getPublicServices());
        }
        return billingResponse;
    }

    public static DpoDataResource toDataProtectionOfficerResponse(DataProtectionOfficer dataProtectionOfficer) {
        DpoDataResource response = null;
        if (dataProtectionOfficer != null) {
            response = new DpoDataResource();
            response.setPec(dataProtectionOfficer.getPec());
            response.setEmail(dataProtectionOfficer.getEmail());
            response.setAddress(dataProtectionOfficer.getAddress());
        }
        return response;
    }

    public static PspDataResource toPaymentServiceProviderResponse(PaymentServiceProvider paymentServiceProvider) {
        PspDataResource response = null;
        if (paymentServiceProvider != null) {
            response = new PspDataResource();
            response.setAbiCode(paymentServiceProvider.getAbiCode());
            response.setLegalRegisterName(paymentServiceProvider.getLegalRegisterName());
            response.setBusinessRegisterNumber(paymentServiceProvider.getBusinessRegisterNumber());
            response.setVatNumberGroup(paymentServiceProvider.getVatNumberGroup());
            response.setLegalRegisterNumber(paymentServiceProvider.getLegalRegisterNumber());
        }
        return response;
    }

}
