package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.constant.TokenType;
import it.pagopa.selfcare.mscore.model.CertifiedField;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.model.onboarding.*;
import it.pagopa.selfcare.mscore.model.user.User;
import it.pagopa.selfcare.mscore.model.user.UserBinding;
import it.pagopa.selfcare.mscore.model.user.UserToOnboard;
import it.pagopa.selfcare.onboarding.common.InstitutionType;

import java.util.ArrayList;
import java.util.List;

public class TestUtils {

    public static ContractImported dummyContractImported() {

        ContractImported contractImported = new ContractImported();
        contractImported.setContractType("Contract Type");
        contractImported.setFileName("foo.txt");
        contractImported.setFilePath("/directory/foo.txt");
        return contractImported;
    }

    public static Token dummyToken() {
        Token token = new Token();
        token.setChecksum("Checksum");
        token.setDeletedAt(null);
        token.setContractSigned("Contract Signed");
        token.setContractTemplate("Contract Template");
        token.setCreatedAt(null);
        token.setExpiringDate(null);
        token.setId("42");
        token.setInstitutionId("42");
        token.setInstitutionUpdate(new InstitutionUpdate());
        token.setProductId("42");
        token.setStatus(RelationshipState.PENDING);
        token.setType(TokenType.INSTITUTION);
        token.setUpdatedAt(null);
        TokenUser user = new TokenUser();
        user.setUserId("id");
        user.setRole(PartyRole.MANAGER);
        token.setUsers(List.of(user));
        return token;
    }

    public static OnboardingRequest dummyOnboardingRequest() {
        OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setInstitutionExternalId("42");
        onboardingRequest.setPricingPlan("Pricing Plan");
        onboardingRequest.setProductId("prod-io");
        onboardingRequest.setProductName("Product Name");
        onboardingRequest.setSignContract(true);
        onboardingRequest.setTokenType(TokenType.INSTITUTION);
        onboardingRequest.setBillingRequest(new Billing());
        onboardingRequest.setUsers(new ArrayList<>());
        return onboardingRequest;
    }

    public static InstitutionUpdate createSimpleInstitutionUpdate() {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate
                .setDataProtectionOfficer(new DataProtectionOfficer("42 Main St", "jane.doe@example.org", "Pec"));
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(new ArrayList<>());
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.PA);
        institutionUpdate
                .setPaymentServiceProvider(new PaymentServiceProvider("Abi Code", "42", "Legal Register Name", "42", true));
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("6625550144");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        return institutionUpdate;
    }

    public static InstitutionUpdate createSimpleInstitutionUpdatePT() {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate
                .setDataProtectionOfficer(new DataProtectionOfficer("42 Main St", "jane.doe@example.org", "Pec"));
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(new ArrayList<>());
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.PT);
        institutionUpdate
                .setPaymentServiceProvider(new PaymentServiceProvider("Abi Code", "42", "Legal Register Name", "42", true));
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("6625550144");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        return institutionUpdate;
    }
    public static InstitutionUpdate createSimpleInstitutionUpdateSA() {
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate
                .setDataProtectionOfficer(new DataProtectionOfficer("42 Main St", "jane.doe@example.org", "Pec"));
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(new ArrayList<>());
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.SA);
        institutionUpdate
                .setPaymentServiceProvider(new PaymentServiceProvider("Abi Code", "42", "Legal Register Name", "42", true));
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("6625550144");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        return institutionUpdate;
    }

    public static AdditionalInformations createSimpleAdditionalInformations(String type) {
        AdditionalInformations additionalInformations = new AdditionalInformations();
        switch (type) {
            case "ipa":
                additionalInformations.setIpa(true);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "regulatedMarket":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(true);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "establishedByRegulatoryProvision":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(true);
                additionalInformations.setAgentOfPublicService(false);
                break;
            case "agentOfPublicService":
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(true);
                break;
            default:
                additionalInformations.setIpa(false);
                additionalInformations.setBelongRegulatedMarket(false);
                additionalInformations.setEstablishedByRegulatoryProvision(false);
                additionalInformations.setAgentOfPublicService(false);
        }

        return additionalInformations;
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

    public static Billing createSimpleBilling() {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");
        return billing;
    }

    public static Contract createSimpleContract() {

        Contract contract = new Contract();
        contract.setPath("Path");
        contract.setVersion("1.0.2");
        return contract;
    }

    public static Institution dummyInstitution() {
        Institution institution = new Institution();
        institution.setId("institutionId");
        institution.setDescription("description");
        institution.setParentDescription("parentDescription");
        institution.setAddress("42 Main St");
        institution.setBusinessRegisterPlace("Business Register Place");
        institution
                .setDataProtectionOfficer(new DataProtectionOfficer("42 Main St", "jane.doe@example.org", "Pec"));
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setImported(true);
        institution
                .setPaymentServiceProvider(new PaymentServiceProvider("Abi Code", "42", "Legal Register Name", "42", true));
        institution.setRea("Rea");
        institution.setShareCapital("Share Capital");
        institution.setSupportEmail("jane.doe@example.org");
        institution.setSupportPhone("6625550144");
        institution.setTaxCode("Tax Code");
        institution.setZipCode("21654");
        return institution;
    }

    public static User createSimpleUser() {
        User user = new User();
        user.setId("1");
        user.setFiscalCode("ABC123XYZ");
        CertifiedField<String> nome = new CertifiedField<>();
        nome.setValue("nome");
        user.setName(nome);
        user.setFamilyName(nome);
        user.setEmail(nome);
        return user;
    }

    public static InstitutionUpdate createDummyInstitutionUpdate(){
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate.setDataProtectionOfficer(createSimpleDataProtectionOfficer());
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(new ArrayList<>());
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.PA);
        institutionUpdate.setPaymentServiceProvider(createSimplePaymentServiceProvider());
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("4105551212");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        return institutionUpdate;
    }public static InstitutionUpdate createDummyInstitutionUpdateGSP(){
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setAddress("42 Main St");
        institutionUpdate.setBusinessRegisterPlace("Business Register Place");
        institutionUpdate.setDataProtectionOfficer(createSimpleDataProtectionOfficer());
        institutionUpdate.setDescription("The characteristics of someone or something");
        institutionUpdate.setDigitalAddress("42 Main St");
        institutionUpdate.setGeographicTaxonomies(new ArrayList<>());
        institutionUpdate.setImported(true);
        institutionUpdate.setInstitutionType(InstitutionType.GSP);
        institutionUpdate.setPaymentServiceProvider(createSimplePaymentServiceProvider());
        institutionUpdate.setRea("Rea");
        institutionUpdate.setShareCapital("Share Capital");
        institutionUpdate.setSupportEmail("jane.doe@example.org");
        institutionUpdate.setSupportPhone("4105551212");
        institutionUpdate.setTaxCode("Tax Code");
        institutionUpdate.setZipCode("21654");
        return institutionUpdate;
    }

    public static OnboardingRequest createDummyOnboardingRequest(){
        OnboardingRequest onboardingRequest = new OnboardingRequest();
        onboardingRequest.setBillingRequest(createSimpleBilling());
        onboardingRequest.setContract(createSimpleContract());
        onboardingRequest.setInstitutionExternalId("42");
        onboardingRequest.setInstitutionUpdate(createDummyInstitutionUpdate());
        onboardingRequest.setPricingPlan("Pricing Plan");
        onboardingRequest.setProductId("42");
        onboardingRequest.setProductName("Product Name");
        onboardingRequest.setSignContract(true);
        onboardingRequest.setContractFilePath("/example");

        return onboardingRequest;
    }
    public static OnboardedUser dummyOnboardedUser(){
        OnboardedUser onboardedUser =  new OnboardedUser();
        onboardedUser.setId("42");
        onboardedUser.setBindings(new ArrayList<>());
        return onboardedUser;
    }

    public static UserBinding dummyUserBinding (){
        UserBinding userBinding = new UserBinding();
        userBinding.setInstitutionId("42");
        userBinding.setInstitutionName("Name");
        userBinding.setInstitutionRootName("RootName");
        userBinding.setProducts(new ArrayList<>());
        return userBinding;
    }

    public static OnboardedProduct dummyOnboardedProduct(){
        OnboardedProduct onboardedProduct =  new OnboardedProduct();
        onboardedProduct.setProductId("prod-interop");
        onboardedProduct.setProductRole("productRole");
        onboardedProduct.setRole(PartyRole.DELEGATE);
        onboardedProduct.setStatus(RelationshipState.ACTIVE);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setRelationshipId("RelId");
        onboardedProduct.setTokenId("tokenId");
        return onboardedProduct;
    }

    public static UserToOnboard dummyUserToOnboard(){
        UserToOnboard userToOnboard = new UserToOnboard();
        userToOnboard.setEmail("prof.einstein@example.org");
        userToOnboard.setEnv(Env.COLL);
        userToOnboard.setId("it.pagopa.selfcare.mscore.model.user.UserToOnboard");
        userToOnboard.setName("42");
        userToOnboard.setProductRole("42");
        userToOnboard.setRole(PartyRole.SUB_DELEGATE);
        userToOnboard.setSurname("it.pagopa.selfcare.mscore.model.user.UserToOnboard");
        userToOnboard.setTaxCode("42");
        return userToOnboard;
    }
}
