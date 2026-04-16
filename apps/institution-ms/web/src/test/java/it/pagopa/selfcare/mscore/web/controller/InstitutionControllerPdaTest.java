package it.pagopa.selfcare.mscore.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.InstitutionService;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.web.model.institution.PdaInstitutionRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ContextConfiguration(classes = {InstitutionController.class})
@ExtendWith(MockitoExtension.class)
public class InstitutionControllerPdaTest {
    @InjectMocks
    private InstitutionController institutionController;

    @Mock
    private InstitutionService institutionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
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
     * Method under test: {@link InstitutionController#createInstitutionFromPda(PdaInstitutionRequest)}}
     */
    @Test
    void shouldCreateInstitutionFromPda() throws Exception {
        // Given
        PdaInstitutionRequest institutionRequest = new PdaInstitutionRequest();
        institutionRequest.setInjectionInstitutionType("EC");
        institutionRequest.setDescription("test ec");
        institutionRequest.setTaxCode("1234");
        institutionRequest.setIstatCode("42");
        String content = objectMapper.writeValueAsString(institutionRequest);

        Institution institution = createSimpleInstitutionPda();

        when(institutionService.createInstitutionFromPda(any(), any())).thenReturn(institution);

        //Then
        MockHttpServletRequestBuilder requestBuilder = post("/institutions/from-pda/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
        ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(institutionController)
                .build()
                .perform(requestBuilder);
        actualPerformResult.andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.content().string("{\"id\":\"42\",\"externalId\":\"42\",\"origin\":\"MOCK\",\"originId\":\"Ipa Code\",\"description\":\"The characteristics of someone or something\",\"address\":\"42 Main St\",\"zipCode\":\"21654\",\"taxCode\":\"1234\",\"istatCode\":\"42\",\"attributes\":[],\"imported\":true,\"delegation\":false}"));
    }

    public static Institution createSimpleInstitutionPda() {
        Institution institution = new Institution();
        institution.setAddress("42 Main St");
        institution.setAttributes(new ArrayList<>());
        institution.setBilling(createSimpleBilling());
        institution.setIstatCode("42");

        institution.setDescription("The characteristics of someone or something");
        institution.setExternalId("42");
        institution.setId("42");
        institution.setOriginId("Ipa Code");
        institution.setOrigin(Origin.MOCK.name());

        institution.setTaxCode("1234");
        institution.setZipCode("21654");
        institution.setImported(true);

        return institution;
    }

    public static Billing createSimpleBilling() {
        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");
        return billing;
    }
}
