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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OnboardingResponseFactoryTest {

    @Inject
    OnboardingResponseFactory factory;

    @InjectMock
    OnboardingMapper mapper;

    @Inject
    @InjectMock
    @RestClient
    UserApi userRegistryApi;

    private static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    private static final String TEST_UUID_TAX_CODE = UUID.randomUUID().toString();
    private static final String TEST_FISCAL_CODE = "PLTGMR96D20H224Z";
    private static final String TEST_NUMERIC_TAX_CODE = "00000000001";

    private Onboarding onboarding;
    private OnboardingGet onboardingDto;
    private Institution institution;

    @BeforeEach
    void setUp() {
        reset(mapper, userRegistryApi);

        onboarding = new Onboarding();
        institution = new Institution();
        institution.setTaxCode(TEST_UUID_TAX_CODE);
        onboarding.setInstitution(institution);

        onboardingDto = new OnboardingGet();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setTaxCode(TEST_UUID_TAX_CODE);
        institutionResponse.setOriginId(TEST_UUID_TAX_CODE);
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
    void toGetResponse_taxCodeIsNotUUID_doesNotCallPdv() {
        institution.setTaxCode(TEST_NUMERIC_TAX_CODE);
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
    void toGetResponse_taxCodeIsPersonalFiscalCode_doesNotCallPdv() {
        institution.setTaxCode(TEST_FISCAL_CODE);
        institution.setInstitutionType(InstitutionType.PRV_PF);

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();

        assertNotNull(result);
        assertEquals(onboardingDto, result);
        verify(mapper, times(1)).toGetResponse(onboarding);
        verifyNoInteractions(userRegistryApi);
    }

    @Test
    void toGetResponse_taxCodeIsUUID_callsPdvToDetokenize() {
        institution.setTaxCode(TEST_UUID_TAX_CODE);
        institution.setInstitutionType(InstitutionType.PRV_PF);

        UserResource userResource = new UserResource();
        userResource.setFiscalCode(TEST_FISCAL_CODE);

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();

        assertNotNull(result);
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getTaxCode());
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getOriginId());

        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE);
    }

    @Test
    void toGetResponse_taxCodeIsUUID_noInstitutionType_callsPdv() {
        institution.setTaxCode(TEST_UUID_TAX_CODE);
        institution.setInstitutionType(null);

        UserResource userResource = new UserResource();
        userResource.setFiscalCode(TEST_FISCAL_CODE);

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();

        assertNotNull(result);
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getTaxCode());
        assertEquals(TEST_FISCAL_CODE, result.getInstitution().getOriginId());

        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE);
    }

    @Test
    void toGetResponse_userRegistryApiException() {
        institution.setTaxCode(TEST_UUID_TAX_CODE);

        RuntimeException testException = new RuntimeException("User registry error");
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE))
                .thenReturn(Uni.createFrom().failure(testException));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();

        assertNotNull(failure);
        assertEquals(testException, failure);

        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE);
    }


    @Test
    void toGetResponse_webApplicationExceptionFromUserRegistry() {
        institution.setTaxCode(TEST_UUID_TAX_CODE);

        WebApplicationException webException = new WebApplicationException("Not found", Response.Status.NOT_FOUND);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE))
                .thenReturn(Uni.createFrom().failure(webException));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        Throwable failure = subscriber.getFailure();

        assertNotNull(failure);
        assertEquals(webException, failure);

        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE);
    }

    @Test
    void toGetResponse_nullFiscalCodeInUserResource() {
        institution.setTaxCode(TEST_UUID_TAX_CODE);

        UserResource userResource = new UserResource();
        userResource.setFiscalCode(null);

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE))
                .thenReturn(Uni.createFrom().item(userResource));

        UniAssertSubscriber<OnboardingGet> subscriber = factory.toGetResponse(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        OnboardingGet result = subscriber.awaitItem().getItem();

        assertNotNull(result);
        assertEquals(onboardingDto, result);
        assertNull(result.getInstitution().getTaxCode());
        assertNull(result.getInstitution().getOriginId());

        verify(mapper, times(1)).toGetResponse(onboarding);
        verify(userRegistryApi, times(1)).findByIdUsingGET(USERS_FIELD_LIST, TEST_UUID_TAX_CODE);
    }

    // --- isUUID tests ---

    @Test
    void isUUID_validUUID_returnsTrue() {
        assertTrue(OnboardingResponseFactory.isUUID(UUID.randomUUID().toString()));
    }

    @Test
    void isUUID_fiscalCode_returnsFalse() {
        assertFalse(OnboardingResponseFactory.isUUID("PLTGMR96D20H224Z"));
    }

    @Test
    void isUUID_numericTaxCode_returnsFalse() {
        assertFalse(OnboardingResponseFactory.isUUID("00000000001"));
    }

    @Test
    void isUUID_null_returnsFalse() {
        assertFalse(OnboardingResponseFactory.isUUID(null));
    }

    @Test
    void isUUID_randomString_returnsFalse() {
        assertFalse(OnboardingResponseFactory.isUUID("not-a-uuid-at-all-but-36-chars!!!!"));
    }
}