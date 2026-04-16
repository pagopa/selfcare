package it.pagopa.selfcare.mscore.web;

import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.model.institution.DataProtectionOfficer;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.PaymentServiceProvider;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionPut;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionRequest;
import it.pagopa.selfcare.mscore.web.model.institution.OnboardingPut;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestUtils {

    public static Billing createSimpleBilling() {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");
        return billing;
    }

    public static DataProtectionOfficer createSimpleDataProtectionOfficer() {

        DataProtectionOfficer dataProtectionOfficer = new DataProtectionOfficer();
        dataProtectionOfficer.setAddress("42 Main St");
        dataProtectionOfficer.setEmail("jane.doe@example.org");
        dataProtectionOfficer.setPec("Pec");
        return dataProtectionOfficer;
    }



    public static PaymentServiceProvider createSimplePaymentServiceProvider() {

        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("Abi Code");
        paymentServiceProvider.setBusinessRegisterNumber("42");
        paymentServiceProvider.setLegalRegisterName("Legal Register Name");
        paymentServiceProvider.setLegalRegisterNumber("42");
        paymentServiceProvider.setVatNumberGroup(true);
        return paymentServiceProvider;
    }

    public static Institution createSimpleInstitutionPA() {
        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(createSimpleBilling());
        institution.setIstatCode("42");

        institution.setDataProtectionOfficer(createSimpleDataProtectionOfficer());
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setOriginId("Ipa Code");
        institution.setOrigin(Origin.MOCK.name());
        institution.setOnboarding(new ArrayList<>());

        institution.setPaymentServiceProvider(createSimplePaymentServiceProvider());
        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        institution.setShareCapital("Share Capital");
        institution.setRea("Rea");

        return institution;
    }

    public static Institution createSimpleInstitutionSA() {
        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(createSimpleBilling());
        institution.setIstatCode("42");

        institution.setDataProtectionOfficer(createSimpleDataProtectionOfficer());
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setOriginId("ANAC Code");
        institution.setOrigin(Origin.ANAC.name());
        institution.setOnboarding(new ArrayList<>());

        institution.setPaymentServiceProvider(createSimplePaymentServiceProvider());
        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        institution.setShareCapital("Share Capital");
        institution.setRea("Rea");

        return institution;
    }

    public static Institution createSimpleInstitutionAS() {
        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(createSimpleBilling());
        institution.setIstatCode("42");

        institution.setDataProtectionOfficer(createSimpleDataProtectionOfficer());
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setOriginId("IVASS Code");
        institution.setOrigin(Origin.IVASS.name());
        institution.setOnboarding(new ArrayList<>());

        institution.setPaymentServiceProvider(createSimplePaymentServiceProvider());
        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        institution.setShareCapital("Share Capital");
        institution.setRea("Rea");

        return institution;
    }

    public static Institution createSimpleInstitutionPG() {
        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setIstatCode("42");

        institution.setDescription("The characteristics of someone or something");
        institution.setExternalId("42");
        institution.setId("42");
        institution.setOriginId("Pg Code");
        institution.setOrigin(Origin.MOCK.name());

       institution.setTaxCode("1234");
        institution.setZipCode("21654");

        return institution;
    }

    public static InstitutionRequest createSimpleInstitutionRequest() {
        InstitutionRequest institution = new InstitutionRequest();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());

        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setInstitutionType(InstitutionType.PA);
        institution.setOriginId("Ipa Code");
        institution.setOrigin(Origin.MOCK.name());
        institution.setOnboarding(new ArrayList<>());

        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        institution.setShareCapital("Share Capital");
        institution.setRea("Rea");
        institution.setLegalForm("legalForm");

        return institution;
    }

    public static InstitutionPut createSimpleInstitutionPut() {
        OnboardingPut onboardingPut = new OnboardingPut();
        onboardingPut.setProductId("productId");
        onboardingPut.setVatNumber("vatNumber");

        InstitutionPut institutionPut = new InstitutionPut();
        institutionPut.setDescription("desc");
        institutionPut.setDigitalAddress("digitalAddress");
        institutionPut.setParentDescription("parentDesc");
        institutionPut.setGeographicTaxonomyCodes(new ArrayList<>());
        institutionPut.setOnboardings(List.of(onboardingPut));
        institutionPut.setAddress("address");
        institutionPut.setZipCode("zipCode");
        institutionPut.setRea("AA-00000");
        institutionPut.setShareCapital("9999");
        institutionPut.setBusinessRegisterPlace("registerPlace");
        institutionPut.setSupportEmail("test@test.test");
        return institutionPut;
    }

    public static Stream<Arguments> getBlankFieldTestCases() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of(null, null),
                Arguments.of("   ", "   "),
                Arguments.of("valid", ""),
                Arguments.of("", "valid"),
                Arguments.of(null, "valid"),
                Arguments.of("valid", null),
                Arguments.of("valid", "   "),
                Arguments.of("   ", "valid")
        );
    }
}
