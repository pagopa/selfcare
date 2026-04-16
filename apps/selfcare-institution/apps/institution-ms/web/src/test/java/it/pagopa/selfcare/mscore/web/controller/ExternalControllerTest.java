package it.pagopa.selfcare.mscore.web.controller;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.core.ExternalService;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.web.model.mapper.*;
import it.pagopa.selfcare.mscore.web.util.SpringContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalControllerTest {
    @InjectMocks
    private ExternalController externalController;

    @Mock
    private ExternalService externalService;

    @Spy
    private InstitutionUpdateMapper institutionUpdateMapper = new InstitutionUpdateMapperImpl();

    @Spy
    private OnboardingResourceMapper onboardingResourceMapper = new OnboardingResourceMapperImpl(institutionUpdateMapper);

    @Spy
    private InstitutionResourceMapper institutionResourceMapper = new InstitutionResourceMapperImpl(onboardingResourceMapper);

    final String fakeJwt = "eyJhbGciOiJIUzI1NiJ9."
            + "eyJhdWQiOlsiYWx0cm8tYXVkaWVuY2UiXSwic3ViIjoidGVzdCJ9."
            + "abc123fakeSignature";

    @BeforeEach
    void setup() {
        // mock application context
        ApplicationContext ctx = mock(ApplicationContext.class);
        SpringContext.setContext(ctx);

        // mock authentication
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getCredentials()).thenReturn(fakeJwt);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }


    /**
     * Method under test: {@link ExternalController#retrieveInstitutionProductsByExternalId(String, List)}
     */
    @Test
    void testRetrieveInstitutionProductsByExternalId() throws Exception {
        when(externalService.retrieveInstitutionProductsByExternalId(any(), any()))
                .thenReturn(new ArrayList<>());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/products", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("{\"products\":[]}"));
    }

    /**
     * Method under test: {@link ExternalController#retrieveInstitutionProductsByExternalId(String, List)}
     */
    @Test
    void testRetrieveInstitutionProductsByExternalId2() throws Exception {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("?");
        billing.setVatNumber("42");

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setClosedAt(null);
        onboarding.setContract("?");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("?");
        onboarding.setProductId("42");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setTokenId("42");
        onboarding.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding);
        when(externalService.retrieveInstitutionProductsByExternalId(any(), any()))
                .thenReturn(onboardingList);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/products", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string("{\"products\":[{\"id\":\"42\",\"state\":\"PENDING\"}]}"));
    }

    /**
     * Method under test: {@link ExternalController#retrieveInstitutionProductsByExternalId(String, List)}
     */
    @Test
    void testRetrieveInstitutionProductsByExternalId3() throws Exception {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("?");
        billing.setVatNumber("42");

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setClosedAt(null);
        onboarding.setContract("?");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("?");
        onboarding.setProductId("42");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setTokenId("42");
        onboarding.setUpdatedAt(null);

        Billing billing1 = new Billing();
        billing1.setPublicServices(false);
        billing1.setRecipientCode("U");
        billing1.setVatNumber("?");

        Onboarding onboarding1 = new Onboarding();
        onboarding1.setBilling(billing1);
        onboarding1.setClosedAt(null);
        onboarding1.setContract("U");
        onboarding1.setCreatedAt(null);
        onboarding1.setPricingPlan("U");
        onboarding1.setProductId("?");
        onboarding1.setStatus(RelationshipState.ACTIVE);
        onboarding1.setTokenId("ABC123");
        onboarding1.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding1);
        onboardingList.add(onboarding);
        when(externalService.retrieveInstitutionProductsByExternalId(any(), any()))
                .thenReturn(onboardingList);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/products", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"products\":[{\"id\":\"?\",\"state\":\"ACTIVE\"},{\"id\":\"42\",\"state\":\"PENDING\"}]}"));
    }

    /**
     * Method under test: {@link ExternalController#retrieveInstitutionGeoTaxonomiesByExternalId(String)}
     */
    @Test
    void testRetrieveInstitutionGeoTaxonomiesByExternalId() throws Exception {
        when(externalService.retrieveInstitutionGeoTaxonomiesByExternalId(any())).thenReturn(new ArrayList<>());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/geotaxonomies", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("[]"));
    }


    /**
     * Method under test: {@link ExternalController#getBillingInstitutionByExternalId(String, String)}
     */
    @Test
    void testGetBillingInstitutionByExternalId3() throws Exception {
        Institution institution = new Institution();
        institution.setOnboarding(new ArrayList<>());
        when(externalService.retrieveInstitutionProduct(any(), any())).thenReturn(institution);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/products/{productId}/billing", "42", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"institutionId\":null,\"externalId\":null,\"origin\":null,\"originId\":null,\"description\":null,\"institutionType\":null,\"digitalAddress\":null,\"address\":null,\"zipCode\":null,\"taxCode\":null,\"pricingPlan\":null,\"billing\":null,\"subunitCode\":null,\"subunitType\":null,\"aooParentCode\":null}"));
    }

    /**
     * Method under test: {@link ExternalController#getBillingInstitutionByExternalId(String, String)}
     */
    @Test
    void testGetBillingInstitutionByExternalId5() throws Exception {
        Institution institution = new Institution();
        institution.setOnboarding(new ArrayList<>());
        when(externalService.retrieveInstitutionProduct(any(), any())).thenReturn(institution);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/external/institutions/{externalId}/products/{productId}/billing", "42", "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string(
                                "{\"institutionId\":null,\"externalId\":null,\"origin\":null,\"originId\":null,\"description\":null,\"institutionType"
                                        + "\":null,\"digitalAddress\":null,\"address\":null,\"zipCode\":null,\"taxCode\":null,\"pricingPlan\":null,\"billing"
                                        + "\":null,\"subunitCode\":null,\"subunitType\":null,\"aooParentCode\":null}"));
    }

    /**
     * Method under test: {@link ExternalController#getByExternalId(String)}
     */
    @Test
    void testGetByExternalId() throws Exception {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        DataProtectionOfficer dataProtectionOfficer = new DataProtectionOfficer();
        dataProtectionOfficer.setAddress("42 Main St");
        dataProtectionOfficer.setEmail("jane.doe@example.org");
        dataProtectionOfficer.setPec("Pec");

        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("Abi Code");
        paymentServiceProvider.setBusinessRegisterNumber("42");
        paymentServiceProvider.setLegalRegisterName("Legal Register Name");
        paymentServiceProvider.setLegalRegisterNumber("42");
        paymentServiceProvider.setVatNumberGroup(true);

        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(billing);
        institution.setDataProtectionOfficer(dataProtectionOfficer);
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setOnboarding(new ArrayList<>());
        institution.setOriginId("42");
        institution.setPaymentServiceProvider(paymentServiceProvider);
        institution.setTaxCode("Tax Code");
        institution.setZipCode("21654");
        when(externalService.getInstitutionByExternalId(any())).thenReturn(institution);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions/{externalId}",
                "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string("{\"id\":\"42\",\"externalId\":\"42\",\"originId\":\"42\",\"description\":\"The characteristics of someone or something\",\"digitalAddress\":\"42 Main St\",\"address\":\"42 Main St\",\"zipCode\":\"21654\",\"taxCode\":\"Tax Code\",\"geographicTaxonomies\":[],\"attributes\":[],\"onboarding\":[],\"paymentServiceProvider\":{\"abiCode\":\"Abi Code\",\"businessRegisterNumber\":\"42\",\"legalRegisterNumber\":\"42\",\"legalRegisterName\":\"Legal Register Name\",\"vatNumberGroup\":true},\"dataProtectionOfficer\":{\"address\":\"42 Main St\",\"email\":\"jane.doe@example.org\",\"pec\":\"Pec\"},\"imported\":false,\"delegation\":false}"));
    }

    /**
     * Method under test: {@link ExternalController#getByExternalId(String)}
     */
    @Test
    void testGetByExternalId2() throws Exception {
        Attributes attributes = new Attributes();
        attributes.setCode("?");
        attributes.setDescription("The characteristics of someone or something");
        attributes.setOrigin("?");

        ArrayList<Attributes> attributesList = new ArrayList<>();
        attributesList.add(attributes);

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        DataProtectionOfficer dataProtectionOfficer = new DataProtectionOfficer();
        dataProtectionOfficer.setAddress("42 Main St");
        dataProtectionOfficer.setEmail("jane.doe@example.org");
        dataProtectionOfficer.setPec("Pec");

        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("Abi Code");
        paymentServiceProvider.setBusinessRegisterNumber("42");
        paymentServiceProvider.setLegalRegisterName("Legal Register Name");
        paymentServiceProvider.setLegalRegisterNumber("42");
        paymentServiceProvider.setVatNumberGroup(true);

        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(attributesList);
        institution.setBilling(billing);
        institution.setDataProtectionOfficer(dataProtectionOfficer);
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(new ArrayList<>());
        institution.setId("42");
        institution.setOnboarding(new ArrayList<>());
        institution.setOriginId("42");
        institution.setPaymentServiceProvider(paymentServiceProvider);
        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        when(externalService.getInstitutionByExternalId("42")).thenReturn(institution);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions/{externalId}",
                "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string("{\"id\":\"42\",\"externalId\":\"42\",\"originId\":\"42\",\"description\":\"The characteristics of someone or something\",\"digitalAddress\":\"42 Main St\",\"address\":\"42 Main St\",\"zipCode\":\"21654\",\"taxCode\":\"1234\",\"geographicTaxonomies\":[],\"attributes\":[{\"origin\":\"?\",\"code\":\"?\",\"description\":\"The characteristics of someone or something\"}],\"onboarding\":[],\"paymentServiceProvider\":{\"abiCode\":\"Abi Code\",\"businessRegisterNumber\":\"42\",\"legalRegisterNumber\":\"42\",\"legalRegisterName\":\"Legal Register Name\",\"vatNumberGroup\":true},\"dataProtectionOfficer\":{\"address\":\"42 Main St\",\"email\":\"jane.doe@example.org\",\"pec\":\"Pec\"},\"imported\":false,\"delegation\":false}"));

    }

    /**
     * Method under test: {@link ExternalController#getByExternalId(String)}
     */
    @Test
    void testGetByExternalId3() throws Exception {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        DataProtectionOfficer dataProtectionOfficer = new DataProtectionOfficer();
        dataProtectionOfficer.setAddress("42 Main St");
        dataProtectionOfficer.setEmail("jane.doe@example.org");
        dataProtectionOfficer.setPec("Pec");

        InstitutionGeographicTaxonomies geographicTaxonomies = new InstitutionGeographicTaxonomies();
        geographicTaxonomies.setCode("?");
        geographicTaxonomies.setDesc("The characteristics of someone or something");

        List<InstitutionGeographicTaxonomies> geographicTaxonomiesList = new ArrayList<>();
        geographicTaxonomiesList.add(geographicTaxonomies);

        PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
        paymentServiceProvider.setAbiCode("Abi Code");
        paymentServiceProvider.setBusinessRegisterNumber("42");
        paymentServiceProvider.setLegalRegisterName("Legal Register Name");
        paymentServiceProvider.setLegalRegisterNumber("42");
        paymentServiceProvider.setVatNumberGroup(true);

        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(billing);
        institution.setDataProtectionOfficer(dataProtectionOfficer);
        institution.setDescription("The characteristics of someone or something");
        institution.setDigitalAddress("42 Main St");
        institution.setExternalId("42");
        institution.setGeographicTaxonomies(geographicTaxonomiesList);
        institution.setId("42");
        institution.setOnboarding(new ArrayList<>());
        institution.setOriginId("42");
        institution.setPaymentServiceProvider(paymentServiceProvider);
        institution.setTaxCode("Tax Code");
        institution.setZipCode("21654");
        when(externalService.getInstitutionByExternalId(any())).thenReturn(institution);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions/{externalId}",
                "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content()
                        .string("{\"id\":\"42\",\"externalId\":\"42\",\"originId\":\"42\",\"description\":\"The characteristics of someone or something\",\"digitalAddress\":\"42 Main St\",\"address\":\"42 Main St\",\"zipCode\":\"21654\",\"taxCode\":\"Tax Code\",\"geographicTaxonomies\":[{\"code\":\"?\",\"desc\":\"The characteristics of someone or something\"}],\"attributes\":[],\"onboarding\":[],\"paymentServiceProvider\":{\"abiCode\":\"Abi Code\",\"businessRegisterNumber\":\"42\",\"legalRegisterNumber\":\"42\",\"legalRegisterName\":\"Legal Register Name\",\"vatNumberGroup\":true},\"dataProtectionOfficer\":{\"address\":\"42 Main St\",\"email\":\"jane.doe@example.org\",\"pec\":\"Pec\"},\"imported\":false,\"delegation\":false}"));
    }

    /**
     * Method under test: {@link ExternalController#getByExternalId(String)}
     */
    @Test
    void testGetByExternalId5() throws Exception {
        when(externalService.getInstitutionByExternalId(any())).thenReturn(new Institution());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions/{externalId}",
                "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("{\"imported\":false,\"delegation\":false}"));
    }


    /**
     * Method under test: {@link ExternalController#getByExternalId(String)}
     */
    @Test
    void testGetByExternalId7() throws Exception {
        when(externalService.getInstitutionByExternalId(any())).thenReturn(new Institution());
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions/{externalId}",
                "42");
        MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("{\"imported\":false,\"delegation\":false}"));
    }



    /**
     * Method under test: {@link ExternalController#retrieveInstitutionByIds(List)}
     */
    @Test
    void testRetrieveInstitutionByIds() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/external/institutions")
                .param("ids","[code1, code2]");
        ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(externalController)
                .build()
                .perform(requestBuilder);
        actualPerformResult.andExpect(MockMvcResultMatchers.status().is(200));
    }
}


