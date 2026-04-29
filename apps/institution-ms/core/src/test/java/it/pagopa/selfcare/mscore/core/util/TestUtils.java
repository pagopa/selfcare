package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.model.onboarding.Contract;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardingRequest;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public class TestUtils {


    public static Institution dummyInstitutionPa() {
        return dummyInstitution(InstitutionType.PA);
    }
    public static Institution dummyInstitutionGsp() {
        return dummyInstitution(InstitutionType.GSP);
    }

    public static Institution dummyInstitutionSa() {
        return dummyInstitution(InstitutionType.SA);
    }

    public static Institution dummyInstitutionAs() {
        return dummyInstitution(InstitutionType.AS);
    }
    public static Institution dummyInstitutionPt() {
        return dummyInstitution(InstitutionType.PT);
    }

    public static Institution dummyInstitutionPg() {
        return dummyInstitution(InstitutionType.PG);
    }

    private static Institution dummyInstitution(InstitutionType institutionType) {

        Billing billing = new Billing();
        ArrayList<Onboarding> onboarding = new ArrayList<>();
        ArrayList<InstitutionGeographicTaxonomies> geographicTaxonomies = new ArrayList<>();
        ArrayList<Attributes> attributes = new ArrayList<>();
        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();

        return Institution.builder()
                .id("42")
                .externalId("42")
                .origin(Origin.SELC.getValue())
                .originId("originId")
                .description("institutionDescription")
                .digitalAddress("institution@test.test")
                .address("42 Main St")
                .zipCode("21654")
                .taxCode("01234567890")
                .ivassCode("ivass")
                .city("city")
                .country("county")
                .county("country")
                .istatCode("istatCode")
                .billing(billing)
                .onboarding(onboarding)
                .geographicTaxonomies(geographicTaxonomies)
                .attributes(attributes)
                .paymentServiceProvider(paymentServiceProvider)
                .dataProtectionOfficer(new DataProtectionOfficer())
                .businessRegisterPlace("place")
                .supportEmail("test@test.test")
                .supportPhone("0000000000")
                .imported(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .paAttributes(new PaAttributes())
                .delegation(false)
                .build();
    }

    public static OnboardingRequest dummyOnboardingRequest(Billing billing, Contract contract, InstitutionUpdate institutionUpdate){
       OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setBillingRequest(billing);
        onboardingRequest.setContract(contract);
        onboardingRequest.setInstitutionExternalId("42");
        onboardingRequest.setInstitutionUpdate(institutionUpdate);
        onboardingRequest.setPricingPlan("Pricing Plan");
        onboardingRequest.setProductId("42");
        onboardingRequest.setProductName("Product Name");
        onboardingRequest.setSignContract(true);
        onboardingRequest.setUsers(new ArrayList<>());
        return onboardingRequest;
    }
}
