package it.pagopa.selfcare.onboarding.factory;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OnboardingResponseFactoryTest {

    @Inject
    OnboardingResponseFactory factory;

    @InjectMock
    OnboardingMapper mapper;

    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    private static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    private static final String TEST_TAX_CODE = "TAXCODE123";
    private static final String TEST_FISCAL_CODE = "FISCAL123";

    private Onboarding onboarding;
    private OnboardingGet onboardingDto;
    private Institution institution;
    private InstitutionResponse institutionResponse;

    @BeforeEach
    void setUp() {
        reset(mapper, userRegistryApi);
        
        // Setup base onboarding entity
        onboarding = new Onboarding();
        institution = new Institution();
        institution.setTaxCode(TEST_TAX_CODE);
        onboarding.setInstitution(institution);

        // Setup DTO response
        onboardingDto = new OnboardingGet();
        institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode(TEST_TAX_CODE);
        institutionResponse.setOriginId(TEST_TAX_CODE);
        onboardingDto.setInstitution(institutionResponse);

        when(mapper.toGetResponse(any(Onboarding.class))).thenReturn(onboardingDto);
    }

    @Test
    void toGetResponse_OnboardingIsNull() {
        when(mapper.toGetResponse(null)).thenReturn(onboardingDto);

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(null)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNotNull(result);
        assertEquals(onboardingDto, result);
        verify(mapper, times(1)).toGetResponse(null);
        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void toGetResponse_InstitutionIsNull() {
        onboarding.setInstitution(null);

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNotNull(result);
        assertEquals(onboardingDto, result);
        verify(mapper, times(1)).toGetResponse(onboarding);
        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void toGetResponse_InstitutionTypeIsNotPrvPf() {
        institution.setInstitutionType(InstitutionType.PA);

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNotNull(result);
        assertEquals(onboardingDto, result);
        verify(mapper, times(1)).toGetResponse(onboarding);
        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void toGetResponse_InstitutionTypeIsPrvPf() {
        institution.setInstitutionType(InstitutionType.PRV_PF);
        
        UserResource userResource = new UserResource();
        userResource.setFiscalCode(TEST_FISCAL_CODE);
        
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNotNull(result);
        assertEquals(onboardingDto, result);
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getTaxCode());
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getOriginId());
        
        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE);
    }

    @Test
    void toGetResponse_userRegistryApiException() {
        institution.setInstitutionType(InstitutionType.PRV_PF);
        
        RuntimeException testException = new RuntimeException("User registry error");
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE))
                .thenReturn(Uni.createFrom().failure(testException));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        
        assertNotNull(failure);
        assertEquals(testException, failure);
        
        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE);
    }

    @Test
    void toGetResponse_mapperReturnNull() {
        when(mapper.toGetResponse(any(Onboarding.class))).thenReturn(null);

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNull(result);
        verify(mapper, times(1)).toGetResponse(onboarding);
        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void toGetResponse_webApplicationExceptionFromUserRegistry() {
        institution.setInstitutionType(InstitutionType.PRV_PF);
        
        WebApplicationException webException = new WebApplicationException("Not found", Response.Status.NOT_FOUND);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE))
                .thenReturn(Uni.createFrom().failure(webException));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();
        
        assertNotNull(failure);
        assertEquals(webException, failure);
        
        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE);
    }

    @Test
    void toGetResponse_nullFiscalCodeInUserResource() {
        institution.setInstitutionType(InstitutionType.PRV_PF);
        
        UserResource userResource = new UserResource();
        userResource.setFiscalCode(null);
        
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();
        
        assertNotNull(result);
        assertEquals(onboardingDto, result);
        assertNull(result.getInstitution().getTaxCode());
        assertNull(result.getInstitution().getOriginId());
        
        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_TAX_CODE);
    }
}