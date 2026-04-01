package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.core_json.api.DelegationApi;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.*;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamereApi;
import org.openapi.quarkus.party_registry_proxy_json.api.NationalRegistriesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.AOOResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class CompletionServiceDefaultTest {

    @Inject
    CompletionServiceDefault completionServiceDefault;

    @InjectMock
    OnboardingRepository onboardingRepository;
    @InjectMock
    TokenRepository tokenRepository;
    @InjectMock
    NotificationService notificationService;
    @InjectMock
    ProductService productService;

    @RestClient
    @InjectMock
    InstitutionApi institutionApi;
    @RestClient
    @InjectMock
    org.openapi.quarkus.user_json.api.UserApi userControllerApi;
    @RestClient
    @InjectMock
    UserApi userRegistryApi;
    @RestClient
    @InjectMock
    AooApi aooApi;
    @RestClient
    @InjectMock
    UoApi uoApi;
    @RestClient
    @InjectMock
    org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi institutionRegistryProxyApi;
    @RestClient
    @InjectMock
    DelegationApi delegationApi;
    @RestClient
    @InjectMock
    org.openapi.quarkus.user_json.api.InstitutionApi userInstitutionApi;
    @RestClient
    @InjectMock
    InfocamereApi infocamereApi;
    @RestClient
    @InjectMock
    NationalRegistriesApi nationalRegistriesApi;

    final String productId = "productId";
    private static final UserResource userResource;

    private OnboardingMapper onboardingMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        onboardingMapper = mock(OnboardingMapper.class);
        objectMapper = mock(ObjectMapper.class);
    }

    static {
        userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        Map<String, WorkContactResource> map = new HashMap<>();
        userResource.setWorkContacts(map);
    }

    @Nested
    @TestProfile(CompletionServiceDefaultTest.UserMSProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class UserMSProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.persist-users.active", "true");
        }
    }

    @Test
    void createInstitutionWithExistingInstitutionId() {
        // given
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setId("id");
        institution.setInstitutionType(InstitutionType.PSP);
        institution.setOrigin(Origin.SELC);
        onboarding.setInstitution(institution);

        // when
        String result = completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        // then
        assertEquals(result, onboarding.getInstitution().getId());
        assertFalse(result.isEmpty());
    }

    @Test
    void createInstitutionAndPersistInstitutionIsNull() {
        // given
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PSP);
        institution.setOrigin(Origin.SELC);
        onboarding.setInstitution(institution);
        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        InstitutionsResponse institutionsResponse = new InstitutionsResponse();

        when(institutionApi.getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(institutionsResponse);

        when(institutionApi.createInstitutionUsingPOST(any()))
                .thenReturn(institutionResponse);

        WebApplicationException e = mock(WebApplicationException.class);
        when(e.getResponse()).thenReturn(Response.status(404).build());

        when(institutionRegistryProxyApi.findInstitutionUsingGET(any(), any(), any())).thenThrow(e);

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(onboardingRepository.update("institution.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        // when
        String result = completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        // then
        verify(institutionApi, times(1)).getInstitutionsUsingGET(any(), any(), any(), any(), any(), any());
        verify(institutionApi, times(1)).createInstitutionUsingPOST(any());
        assertEquals(result, institutionResponse.getId());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_foundInstitution() {
        Onboarding onboarding = createOnboarding();

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createOrRetrieveInstitutionSuccess() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());
        // Verify that updateInstitutionUsingPUT is not called when rea, businessRegisterPlace and shareCapital are not set
        verify(institutionApi, never()).updateInstitutionUsingPUT(any(), any());
    }

    @Test
    void createOrRetrieveInstitutionSuccess_withReaField_shouldUpdateInstitution() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        institution.setRea("REA123");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());

        ArgumentCaptor<InstitutionPut> captor = ArgumentCaptor.forClass(InstitutionPut.class);
        verify(institutionApi, times(1)).updateInstitutionUsingPUT(eq("actual-id"), captor.capture());
        assertEquals("REA123", captor.getValue().getRea());
        assertNull(captor.getValue().getBusinessRegisterPlace());
        assertNull(captor.getValue().getShareCapital());
    }

    @Test
    void createOrRetrieveInstitutionSuccess_withBusinessRegisterPlace_shouldUpdateInstitution() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        institution.setBusinessRegisterPlace("Milano");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());

        ArgumentCaptor<InstitutionPut> captor = ArgumentCaptor.forClass(InstitutionPut.class);
        verify(institutionApi, times(1)).updateInstitutionUsingPUT(eq("actual-id"), captor.capture());
        assertEquals("Milano", captor.getValue().getBusinessRegisterPlace());
        assertNull(captor.getValue().getRea());
        assertNull(captor.getValue().getShareCapital());
    }

    @Test
    void createOrRetrieveInstitutionSuccess_withShareCapital_shouldUpdateInstitution() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        institution.setShareCapital("10000");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());

        ArgumentCaptor<InstitutionPut> captor = ArgumentCaptor.forClass(InstitutionPut.class);
        verify(institutionApi, times(1)).updateInstitutionUsingPUT(eq("actual-id"), captor.capture());
        assertEquals("10000", captor.getValue().getShareCapital());
        assertNull(captor.getValue().getRea());
        assertNull(captor.getValue().getBusinessRegisterPlace());
    }

    @Test
    void createOrRetrieveInstitutionSuccess_withSupportEmail_shouldUpdateInstitution() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        institution.setSupportEmail("support@test.it");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());

        ArgumentCaptor<InstitutionPut> captor = ArgumentCaptor.forClass(InstitutionPut.class);
        verify(institutionApi, times(1)).updateInstitutionUsingPUT(eq("actual-id"), captor.capture());
        assertEquals("support@test.it", captor.getValue().getSupportEmail());
        assertNull(captor.getValue().getRea());
        assertNull(captor.getValue().getBusinessRegisterPlace());
        assertNull(captor.getValue().getShareCapital());
    }

    @Test
    void createOrRetrieveInstitutionSuccess_withAllFields_shouldUpdateInstitution() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        institution.setRea("REA123");
        institution.setBusinessRegisterPlace("Milano");
        institution.setShareCapital("10000");
        institution.setSupportEmail("support@test.it");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        institutionResponse.setId("actual-id");
        response.setInstitutions(List.of(institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse serviceResponse = completionServiceDefault.createOrRetrieveInstitution(onboarding);

        assertNotNull(serviceResponse);
        assertEquals("actual-id", serviceResponse.getId());

        ArgumentCaptor<InstitutionPut> captor = ArgumentCaptor.forClass(InstitutionPut.class);
        verify(institutionApi, times(1)).updateInstitutionUsingPUT(eq("actual-id"), captor.capture());
        assertEquals("REA123", captor.getValue().getRea());
        assertEquals("Milano", captor.getValue().getBusinessRegisterPlace());
        assertEquals("10000", captor.getValue().getShareCapital());
        assertEquals("support@test.it", captor.getValue().getSupportEmail());
    }

    @Test
    void createOrRetrieveInstitutionFailure() {
        Onboarding onboarding = createOnboarding();
        Institution institution = new Institution();
        institution.setId("actual-id");
        institution.setTaxCode("123");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        InstitutionResponse institutionResponse = new InstitutionResponse();
        response.setInstitutions(List.of(institutionResponse, institutionResponse));

        when(institutionApi.getInstitutionsUsingGET(institution.getTaxCode(), null, null, null, null, null))
                .thenReturn(response);

        assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.createOrRetrieveInstitution(onboarding));
    }

    void mockOnboardingUpdateAndExecuteCreateInstitution(Onboarding onboarding) {
        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("institution.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        verify(onboardingRepository, times(1))
                .update("institution.id = ?1 and updatedAt = ?2 ", any(), any());
    }

    @Test
    void persistUpadatedAt() {
        Onboarding onboarding = createOnboarding();

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("activatedAt.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.persistActivatedAt(onboarding);

        verify(onboardingRepository, times(1))
                .update("activatedAt = ?1 and updatedAt = ?2 ", any(), any());
    }

    @Test
    void rejectOutdatedOnboardings() {

        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setOriginId("originId");
        onboarding.getInstitution().setOrigin(Origin.IPA);

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("productId = ?1 and institution.origin = ?2 and institution.originId = ?3 and status = PENDING or status = TOBEVALIDATED",
                onboarding.getProductId(), onboarding.getInstitution().getOrigin(), onboarding.getInstitution().getOriginId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("status = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        completionServiceDefault.rejectOutdatedOnboardings(onboarding);

        verify(onboardingRepository, times(1))
                .update("status = ?1 and updatedAt = ?2 ", any(), any());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateSaAnac() {
        Onboarding onboarding = createOnboarding();

        Institution institutionSa = new Institution();
        institutionSa.setTaxCode("taxCode");
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setOrigin(Origin.ANAC);
        onboarding.setInstitution(institutionSa);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromAnacUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateAsIvassWithTaxCode() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setInstitutionType(InstitutionType.AS);
        institution.setOrigin(Origin.IVASS);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIvassUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreateAsIvassWithOrigin() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.AS);
        institution.setOrigin(Origin.IVASS);
        institution.setOriginId("originId");
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(null, null, Origin.IVASS.getValue(), "originId", null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIvassUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePgAde() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setInstitutionType(InstitutionType.PG);
        institution.setOrigin(Origin.ADE);
        onboarding.setInstitution(institution);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromInfocamereUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePaAOO() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setSubunitType(InstitutionPaSubunitType.AOO);
        institution.setSubunitCode("code");
        institution.setRea("REA123");
        institution.setShareCapital("10000");
        institution.setBusinessRegisterPlace("Milano");
        onboarding.setInstitution(institution);

        AOOResource aooResource = new AOOResource();
        when(aooApi.findByUnicodeUsingGET(institution.getSubunitCode(), null))
                .thenReturn(aooResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> subunitCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(aooApi, times(1))
                .findByUnicodeUsingGET(subunitCodeCaptor.capture(), any());
        assertEquals(institution.getSubunitCode(), subunitCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getSubunitCode(), captor.getValue().getSubunitCode());
        assertEquals(institution.getRea(), captor.getValue().getRea());
        assertEquals(institution.getShareCapital(), captor.getValue().getShareCapital());
        assertEquals(institution.getBusinessRegisterPlace(), captor.getValue().getBusinessRegisterPlace());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePaUO() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setTaxCode("taxCode");
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setSubunitType(InstitutionPaSubunitType.UO);
        institution.setSubunitCode("code");
        institution.setRea("REA123");
        institution.setShareCapital("10000");
        institution.setBusinessRegisterPlace("Milano");
        onboarding.setInstitution(institution);

        UOResource uoResource = new UOResource();
        when(uoApi.findByUnicodeUsingGET1(institution.getSubunitCode(), institution.getInstitutionType().name()))
                .thenReturn(uoResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(), null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> subunitCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(uoApi, times(1))
                .findByUnicodeUsingGET1(subunitCodeCaptor.capture(), any());
        assertEquals(institution.getSubunitCode(), subunitCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getSubunitCode(), captor.getValue().getSubunitCode());
        assertEquals(institution.getRea(), captor.getValue().getRea());
        assertEquals(institution.getShareCapital(), captor.getValue().getShareCapital());
        assertEquals(institution.getBusinessRegisterPlace(), captor.getValue().getBusinessRegisterPlace());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundInstitutionAndCreatePa() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setTaxCode("taxCode");
        institution.setRea("REA123");
        institution.setShareCapital("10000");
        institution.setBusinessRegisterPlace("Milano");
        onboarding.setInstitution(institution);

        InstitutionResource institutionResource = new InstitutionResource();
        when(institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null, null))
                .thenReturn(institutionResource);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                null, null, null, null, null))
                .thenReturn(response);

        InstitutionResponse institutionResponse = dummyInstitutionResponse();
        when(institutionApi.createInstitutionFromIpaUsingPOST(any())).thenReturn(institutionResponse);

        mockOnboardingUpdateAndExecuteCreateInstitution(onboarding);

        ArgumentCaptor<InstitutionFromIpaPost> captor = ArgumentCaptor.forClass(InstitutionFromIpaPost.class);
        ArgumentCaptor<String> taxCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(institutionRegistryProxyApi, times(1))
                .findInstitutionUsingGET(taxCodeCaptor.capture(), any(), any());
        assertEquals(institution.getTaxCode(), taxCodeCaptor.getValue());
        verify(institutionApi, times(1))
                .createInstitutionFromIpaUsingPOST(captor.capture());
        assertEquals(institution.getTaxCode(), captor.getValue().getTaxCode());
        assertEquals(institution.getRea(), captor.getValue().getRea());
        assertEquals(institution.getShareCapital(), captor.getValue().getShareCapital());
        assertEquals(institution.getBusinessRegisterPlace(), captor.getValue().getBusinessRegisterPlace());
    }

    @Test
    void createInstitutionAndPersistInstitutionId_notFoundOnIpaThrowsResourceNotFoundException() {
        Onboarding onboarding = createOnboarding();

        Institution institution = new Institution();
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.GSP);
        onboarding.setInstitution(institution);

        WebApplicationException e = new WebApplicationException(404);
        when(institutionRegistryProxyApi.findInstitutionUsingGET(institution.getTaxCode(), null, null))
                .thenThrow(e);

        InstitutionsResponse response = new InstitutionsResponse();
        when(institutionApi.getInstitutionsUsingGET(onboarding.getInstitution().getTaxCode(),
                null, Origin.IPA.name(), null, null, null))
                .thenReturn(response);

        assertThrows(ResourceNotFoundException.class,
                () -> completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding));

        verify(institutionApi, never()).createInstitutionUsingPOST(any());
        verify(institutionApi, never()).createInstitutionFromIpaUsingPOST(any());
    }


    void mockOnboardingUpdateWhenPersistOnboarding(Onboarding onboarding) {
        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(panacheUpdateMock.where("_id", onboarding.getId()))
                .thenReturn(Long.valueOf(1));
        when(onboardingRepository.update("activatedAt = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);
    }

    @Test
    void persistOnboarding_emailIsEmpty() {
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setOrigin(Origin.SELC);
        onboarding.getInstitution().setOriginId("originId");
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV);

        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        mockOnboardingUpdateWhenPersistOnboarding(onboarding);

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(productId, actual.getProductId());
    }

    @Test
    void persistOnboarding() {
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setOrigin(Origin.SELC);
        onboarding.getInstitution().setOriginId("originId");
        onboarding.getInstitution().setInstitutionType(InstitutionType.PRV);
        onboarding.setActivatedAt(LocalDateTime.now());
        when(institutionApi.onboardingInstitutionUsingPOST(any(), any()))
                .thenReturn(new InstitutionResponse());
        Token token = new Token();
        token.setContractSigned("contract-signed-path");
        when(tokenRepository.findByOnboardingId(onboarding.getId()))
                .thenReturn(Optional.of(token));

        mockOnboardingUpdateWhenPersistOnboarding(onboarding);

        completionServiceDefault.persistOnboarding(onboarding);

        ArgumentCaptor<InstitutionOnboardingRequest> captor = ArgumentCaptor.forClass(InstitutionOnboardingRequest.class);
        verify(institutionApi, times(1))
                .onboardingInstitutionUsingPOST(any(), captor.capture());

        verify(tokenRepository, times(1))
                .findByOnboardingId(onboarding.getId());

        InstitutionOnboardingRequest actual = captor.getValue();
        assertEquals(onboarding.getProductId(), actual.getProductId());
        assertEquals(onboarding.getPricingPlan(), actual.getPricingPlan());
        assertEquals(token.getContractSigned(), actual.getContractPath());
        assertEquals(onboarding.getIsAggregator(), actual.getIsAggregator());
        assertEquals(actual.getActivatedAt().getDayOfYear(), onboarding.getActivatedAt().getDayOfYear());
    }

    @Test
    void sendCompletedEmail() {

        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
        User user = createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendCompletedEmail(any(), any(), any(), any(), any());

        completionServiceDefault.sendCompletedEmail(onboardingWorkflow);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmail(any(), any(), any(), any(), any());
    }

    @Test
    void sendMailRejection() {

        Product product = createDummyProduct();
        Onboarding onboarding = createOnboarding();
        createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(productService.getProduct(onboarding.getProductId()))
                .thenReturn(product);
        doNothing().when(notificationService).sendMailRejection(any(), any(), any());
        completionServiceDefault.sendMailRejection(context, onboarding);

        Mockito.verify(notificationService, times(1))
                .sendMailRejection(any(), any(), any());
    }

    @Test
    void sendCompletedEmailAggregate() {

        Onboarding onboarding = createOnboarding();
        Aggregator aggregator = new Aggregator();
        aggregator.setDescription("description");
        onboarding.setAggregator(aggregator);

        User user = createDummyUser(onboarding);

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
                .thenReturn(userResource);
        doNothing().when(notificationService).sendCompletedEmailAggregate(any(), any());

        completionServiceDefault.sendCompletedEmailAggregate(onboarding);

        Mockito.verify(notificationService, times(1))
                .sendCompletedEmailAggregate(any(), any());
    }

    @Test
    void persistUsers() {
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(false);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);
        onboarding.setDelegationId("delegationId");

        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);
        completionServiceDefault.persistUsers(onboarding);

        Mockito.verify(userControllerApi, times(1))
                .createUserByUserId(any(), any());
    }

    @Test
    void persistUsers_skip() {
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(true);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);
        onboarding.setDelegationId("delegationId");

        when(productService.getProduct(any())).thenReturn(product);
        completionServiceDefault.persistUsers(onboarding);

        Mockito.verifyNoInteractions(userControllerApi);
    }

    @Test
    void persistUsersWithException() {
        Onboarding onboarding = createOnboarding();
        createDummyUser(onboarding);
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(true);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);

        Response response = new ServerResponse(null, 500, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);

        assertThrows(RuntimeException.class, () -> completionServiceDefault.persistUsers(onboarding));

    }

    @Test
    void persistUsers_withAggregatorAndNotProdPNAndWorkflowTypeIsImport() {
        // Given
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(false);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);
        
        Onboarding onboarding = createOnboarding();
        onboarding.setProductId("product-not-pn");
        onboarding.setIsAggregator(true);
        onboarding.setWorkflowType(WorkflowType.IMPORT_AGGREGATION);
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);
        
        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);
        
        ArgumentCaptor<org.openapi.quarkus.user_json.model.AddUserRoleDto> captor = 
                ArgumentCaptor.forClass(org.openapi.quarkus.user_json.model.AddUserRoleDto.class);
        
        // When
        completionServiceDefault.persistUsers(onboarding);
        
        // Then
        verify(userControllerApi, times(1)).createUserByUserId(anyString(), captor.capture());
        org.openapi.quarkus.user_json.model.AddUserRoleDto capturedDto = captor.getValue();
        assertEquals(true, capturedDto.getProduct().getToAddOnAggregates(),
                "toAddOnAggregates should be true when isAggregator=true and productId is not PROD_PN");
    }

    @Test
    void persistUsers_withAggregatorAndNotProdPNAndWorkflowTypeIsNotImport() {
        // Given
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(false);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);

        Onboarding onboarding = createOnboarding();
        onboarding.setProductId("product-not-pn");
        onboarding.setIsAggregator(true);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);

        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);

        ArgumentCaptor<org.openapi.quarkus.user_json.model.AddUserRoleDto> captor =
                ArgumentCaptor.forClass(org.openapi.quarkus.user_json.model.AddUserRoleDto.class);

        // When
        completionServiceDefault.persistUsers(onboarding);

        // Then
        verify(userControllerApi, times(1)).createUserByUserId(anyString(), captor.capture());
        org.openapi.quarkus.user_json.model.AddUserRoleDto capturedDto = captor.getValue();
        assertEquals(false, capturedDto.getProduct().getToAddOnAggregates());
    }

    @Test
    void persistUsers_withAggregatorAndProdPN() {
        // Given
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(false);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);
        
        Onboarding onboarding = createOnboarding();
        onboarding.setProductId("prod-pn");
        onboarding.setIsAggregator(true);
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);
        
        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);
        
        ArgumentCaptor<org.openapi.quarkus.user_json.model.AddUserRoleDto> captor = 
                ArgumentCaptor.forClass(org.openapi.quarkus.user_json.model.AddUserRoleDto.class);
        
        // When
        completionServiceDefault.persistUsers(onboarding);
        
        // Then
        verify(userControllerApi, times(1)).createUserByUserId(anyString(), captor.capture());
        org.openapi.quarkus.user_json.model.AddUserRoleDto capturedDto = captor.getValue();
        assertEquals(false, capturedDto.getProduct().getToAddOnAggregates(),
                "toAddOnAggregates should be false when productId is PROD_PN even if isAggregator=true");
    }

    @Test
    void persistUsers_withoutAggregator() {
        // Given
        Product product = mock(Product.class);
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setSkipUserCreation(false);
        Map<PartyRole, ProductRoleInfo> roleMappings = Map.of(PartyRole.MANAGER, productRoleInfo);
        when(product.getRoleMappings(anyString())).thenReturn(roleMappings);
        
        Onboarding onboarding = createOnboarding();
        onboarding.setProductId("any-product");
        onboarding.setIsAggregator(false);
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
        createDummyUser(onboarding);
        
        Response response = new ServerResponse(null, 200, null);
        when(userControllerApi.createUserByUserId(any(), any())).thenReturn(response);
        when(productService.getProduct(any())).thenReturn(product);
        
        ArgumentCaptor<org.openapi.quarkus.user_json.model.AddUserRoleDto> captor = 
                ArgumentCaptor.forClass(org.openapi.quarkus.user_json.model.AddUserRoleDto.class);
        
        // When
        completionServiceDefault.persistUsers(onboarding);
        
        // Then
        verify(userControllerApi, times(1)).createUserByUserId(anyString(), captor.capture());
        org.openapi.quarkus.user_json.model.AddUserRoleDto capturedDto = captor.getValue();
        assertEquals(false, capturedDto.getProduct().getToAddOnAggregates(),
                "toAddOnAggregates should be false when isAggregator=false");
    }

    @Test
    void createDelegation() {
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setId("institution-id");
        onboarding.getInstitution().setDescription("institution-description");
        onboarding.getInstitution().setParentDescription("parent-description");
        Aggregator aggregator = new Aggregator();
        aggregator.setDescription("aggregator-description");
        aggregator.setId("aggregator-id");
        onboarding.setAggregator(aggregator);

        DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponse.setId("delegation-id");

        ArgumentCaptor<DelegationRequest> capture = ArgumentCaptor.forClass(DelegationRequest.class);
        when(delegationApi.createDelegationUsingPOST(capture.capture()))
                .thenReturn(delegationResponse);

        String delegationId = completionServiceDefault.createDelegation(onboarding);

        Assertions.assertEquals(onboarding.getInstitution().getId(), capture.getValue().getFrom());
        Assertions.assertEquals(onboarding.getInstitution().getDescription(), capture.getValue().getInstitutionFromName());
        Assertions.assertEquals(onboarding.getAggregator().getId(), capture.getValue().getTo());
        Assertions.assertEquals(onboarding.getAggregator().getDescription(), capture.getValue().getInstitutionToName());
        Assertions.assertEquals(onboarding.getProductId(), capture.getValue().getProductId());
        Assertions.assertEquals(onboarding.getInstitution().getParentDescription(), capture.getValue().getInstitutionFromRootName());
        Assertions.assertEquals("EA", capture.getValue().getType().name());
        Assertions.assertEquals("delegation-id", delegationId);
        Mockito.verify(delegationApi, times(1))
                .createDelegationUsingPOST(capture.capture());
    }

    @Test
    void createDelegationWithNullAggregator() {
        Onboarding onboarding = createOnboarding();
        onboarding.getInstitution().setId("institution-id");
        onboarding.getInstitution().setDescription("institution-description");

        Assertions.assertThrows(GenericOnboardingException.class,
                () -> completionServiceDefault.createDelegation(onboarding),
                "Aggregator is null, impossible to create delegation");
        Mockito.verifyNoInteractions(delegationApi);
    }

    @Test
    void testCreateAggregateOnboardingRequest() {
        // Given
        OnboardingAggregateOrchestratorInput input = createSampleOnboardingInput();
        Onboarding onboardingToUpdate = createSampleOnboarding();

        // When
        String onboardingId = completionServiceDefault.createAggregateOnboardingRequest(input);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        assertNotEquals(onboardingToUpdate.getId(), onboardingId);
        assertEquals(input.getAggregate().getTaxCode(), onboardingToUpdate.getInstitution().getTaxCode());
    }

    @Test
    void testCreateAggregateOnboardingRequestWithoutInstitutionId() {
        // Given
        OnboardingAggregateOrchestratorInput input = createSampleOnboardingInput();
        input.getInstitution().setId(null);
        Onboarding onboardingToUpdate = createSampleOnboarding();
        Institution aggregator = new Institution();
        Onboarding onboardingAggregator = new Onboarding();
        onboardingAggregator.setInstitution(aggregator);
        List<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboardingAggregator);

        // When
        when(onboardingRepository.findByFilters(any(), eq(null), any(), any(), any())).thenReturn(onboardingList);
        when(onboardingRepository.findById(any())).thenReturn(onboardingAggregator);
        doNothing().when(onboardingRepository).persistOrUpdate(any(Onboarding.class));
        String onboardingId = completionServiceDefault.createAggregateOnboardingRequest(input);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        assertNotEquals(onboardingToUpdate.getId(), onboardingId);
        assertEquals(input.getAggregate().getTaxCode(), onboardingToUpdate.getInstitution().getTaxCode());
    }

    public static OnboardingAggregateOrchestratorInput createSampleOnboardingInput() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();

        input.setId("1");
        input.setProductId("productId");
        input.setTestEnvProductIds(Collections.singletonList("testEnvProductId"));
        input.setPricingPlan("pricingPlan");

        Billing billing = new Billing();
        input.setBilling(billing);

        input.setSignContract(true);
        input.setExpiringDate(LocalDateTime.MAX);
        input.setUserRequestUid("example-uid");
        input.setWorkflowInstanceId("workflowInstanceId");
        input.setCreatedAt(LocalDateTime.MAX);
        input.setUpdatedAt(LocalDateTime.MAX);
        input.setActivatedAt(LocalDateTime.MAX);
        input.setDeletedAt(null);
        input.setReasonForReject(null);

        Institution institution = new Institution();
        institution.setOrigin(Origin.IPA);
        institution.setInstitutionType(InstitutionType.PA);
        institution.setId("institutionId");
        institution.setDescription("description");
        institution.setTaxCode("taxCode");
        input.setInstitution(institution);

        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCodeAggregate");
        aggregate.setDescription("descriptionAggregate");
        input.setAggregate(aggregate);

        User user = new User();
        input.setUsers(Collections.singletonList(user));

        return input;
    }

    // Method to create sample Onboarding
    public static Onboarding createSampleOnboarding() {
        Onboarding onboarding = new Onboarding();

        onboarding.setId("1");
        onboarding.setProductId("productId");
        onboarding.setTestEnvProductIds(Collections.singletonList("testEnvProductId"));
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION_AGGREGATE);
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        institution.setOrigin(Origin.IPA);
        institution.setDescription("descriptionAggregate");
        institution.setTaxCode("taxCodeAggregate");
        onboarding.setInstitution(institution);
        onboarding.setUsers(Collections.singletonList(new User()));
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setBilling(new Billing());
        onboarding.setSignContract(true);
        onboarding.setExpiringDate(LocalDateTime.MAX);
        onboarding.setStatus(OnboardingStatus.PENDING);
        onboarding.setUserRequester(UserRequester.builder().userRequestUid("example-uid").build());
        onboarding.setWorkflowInstanceId("workflowInstanceId");
        onboarding.setCreatedAt(LocalDateTime.MAX);
        onboarding.setUpdatedAt(LocalDateTime.MAX);
        onboarding.setActivatedAt(LocalDateTime.MAX);
        onboarding.setDeletedAt(null);
        onboarding.setReasonForReject(null);

        Aggregator aggregator = new Aggregator();
        aggregator.setId("institutionId");
        aggregator.setDescription("description");
        aggregator.setTaxCode("taxCode");
        onboarding.setAggregator(aggregator);

        return onboarding;
    }

    @Test
    void sendTestEmail() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());

        doNothing().when(notificationService).sendTestEmail(executionContext);

        completionServiceDefault.sendTestEmail(executionContext);

        Mockito.verify(notificationService, times(1))
                .sendTestEmail(executionContext);
    }

    @Test
    void checkExistsDelegationTrue() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);

        Institution aggregator = new Institution();
        aggregator.setId("aggregatorId");
        input.setInstitution(aggregator);

        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        DelegationResponse delegation = new DelegationResponse();
        delegation.setStatus(DelegationResponse.StatusEnum.ACTIVE);
        delegationWithPaginationResponse.setDelegations(List.of(delegation));

        when(delegationApi.getDelegationsUsingGET1(null, input.getInstitution().getId(), null, null, aggregate.getTaxCode(), null, null, null))
                .thenReturn(delegationWithPaginationResponse);

        String result = completionServiceDefault.existsDelegation(input);

        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    void checkExistsDelegationError() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);

        Institution aggregator = new Institution();
        aggregator.setId("aggregatorId");
        input.setInstitution(aggregator);

        when(delegationApi.getDelegationsUsingGET1(null, input.getInstitution().getId(), null, null, aggregate.getTaxCode(), null, null, null))
                .thenThrow(WebApplicationException.class);

        Assertions.assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.existsDelegation(input));
    }

    @Test
    void checkExistsDelegationFalse() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);

        Institution aggregator = new Institution();
        aggregator.setId("aggregatorId");
        input.setInstitution(aggregator);

        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        delegationWithPaginationResponse.setDelegations(Collections.emptyList());
        when(delegationApi.getDelegationsUsingGET1(null, aggregator.getId(), null, null, aggregator.getTaxCode(), null, null, null))
                .thenReturn(delegationWithPaginationResponse);

        String result = completionServiceDefault.existsDelegation(input);

        assertFalse(Boolean.parseBoolean(result));
    }

    @Test
    void checkExistsDelegationFalseWithNullInstitutionId() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        Institution aggregator = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);
        input.setInstitution(aggregator);
        Onboarding onboardingAggregator = new Onboarding();

        input.getInstitution().setOrigin(Origin.IPA);
        input.getInstitution().setOriginId("originId");
        input.getInstitution().setTaxCode("taxCode");
        onboardingAggregator.setInstitution(aggregator);
        input.setProductId("prod-io");

        List<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboardingAggregator);

        when(onboardingRepository.findByFilters(any(), eq(null), any(), any(), any())).thenReturn(onboardingList);
        when(onboardingRepository.findById(any())).thenReturn(onboardingAggregator);
        doNothing().when(onboardingRepository).persistOrUpdate(any(Onboarding.class));

        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        delegationWithPaginationResponse.setDelegations(Collections.emptyList());
        when(delegationApi.getDelegationsUsingGET1(null, aggregator.getId(), null, null, aggregator.getTaxCode(), null, null, null))
                .thenReturn(delegationWithPaginationResponse);

        String result = completionServiceDefault.existsDelegation(input);

        assertFalse(Boolean.parseBoolean(result));
    }

    @Test
    void checkExistsDelegationFalseOnboardingNotFound() {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();
        Institution aggregate = new Institution();
        Institution aggregator = new Institution();
        aggregate.setTaxCode("taxCode");
        input.setAggregate(aggregate);
        input.setInstitution(aggregator);

        input.getInstitution().setOrigin(Origin.IPA);
        input.getInstitution().setOriginId("originId");
        input.getInstitution().setTaxCode("taxCode");
        input.setProductId("prod-io");

        when(onboardingRepository.findByFilters(any(), eq(null), any(), any(), any())).thenReturn(List.of());

        Assertions.assertThrows(GenericOnboardingException.class, () -> completionServiceDefault.existsDelegation(input));

    }

    @Test
    void institutionAndAggregateHaveIds() throws Exception {
        OnboardingAggregateOrchestratorInput input = buildInput("inst123", "agg123");
        Onboarding onboarding = new Onboarding();
        when(onboardingMapper.mapToOnboarding(input)).thenReturn(onboarding);
        when(objectMapper.writeValueAsString(onboarding)).thenReturn("json");

        String result = completionServiceDefault.verifyOnboardingAggregate(input);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"productId\":\"product1\""));
        assertTrue(result.contains("\"institution\":{\"id\":\"agg123\""));
        assertTrue(result.contains("\"aggregator\":{\"id\":\"inst123\""));
    }

    @Test
    void institutionIdNull_foundInDb() throws Exception {
        OnboardingAggregateOrchestratorInput input = buildInput(null, "agg123");
        Institution institutionFromDb = new Institution();
        institutionFromDb.setId("foundInstId");
        Onboarding onboardingDb = new Onboarding();
        onboardingDb.setInstitution(institutionFromDb);
        when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(onboardingDb));

        Onboarding onboarding = new Onboarding();
        when(onboardingMapper.mapToOnboarding(input)).thenReturn(onboarding);
        when(objectMapper.writeValueAsString(onboarding)).thenReturn("json");

        String result = completionServiceDefault.verifyOnboardingAggregate(input);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"productId\":\"product1\""));
        assertTrue(result.contains("\"institution\":{\"id\":\"agg123\""));
        assertEquals("foundInstId", input.getInstitution().getId());
    }

    @Test
    void institutionIdNull_notFoundInDb() {
        OnboardingAggregateOrchestratorInput input = buildInput(null, "agg123");
        when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        GenericOnboardingException exception = assertThrows(GenericOnboardingException.class,
                () -> completionServiceDefault.verifyOnboardingAggregate(input));
        assertEquals("Onboarding not found", exception.getMessage());
    }

    @Test
    void aggregateIdNull_foundInDb() throws Exception {
        OnboardingAggregateOrchestratorInput input = buildInput("inst123", null);
        Institution aggregateFromDb = new Institution();
        aggregateFromDb.setId("aggFound");
        Onboarding onboardingDb = new Onboarding();
        onboardingDb.setInstitution(aggregateFromDb);
        when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(onboardingDb));

        Onboarding onboarding = new Onboarding();
        when(onboardingMapper.mapToOnboarding(input)).thenReturn(onboarding);
        when(objectMapper.writeValueAsString(onboarding)).thenReturn("json");

        String result = completionServiceDefault.verifyOnboardingAggregate(input);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"productId\":\"product1\""));
        assertTrue(result.contains("\"aggregator\":{\"id\":\"inst123\""));
        assertEquals("aggFound", input.getAggregate().getId());
    }

    @Test
    void aggregateIdNull_notFoundInDb() throws Exception {
        OnboardingAggregateOrchestratorInput input = buildInput("inst123", null);
        when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Onboarding onboarding = new Onboarding();
        when(onboardingMapper.mapToOnboarding(input)).thenReturn(onboarding);
        when(objectMapper.writeValueAsString(onboarding)).thenReturn("json");

        String result = completionServiceDefault.verifyOnboardingAggregate(input);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"productId\":\"product1\""));
        assertTrue(result.contains("\"aggregator\":{\"id\":\"inst123\""));
        assertNull(input.getAggregate().getId());
    }

    @Test
    void bothIdsNull_bothFoundInDb() throws Exception {
        OnboardingAggregateOrchestratorInput input = buildInput(null, null);
        Institution inst = new Institution();
        inst.setId("instId");
        Institution agg = new Institution();
        agg.setId("aggId");

        Onboarding onboardingInstitution = new Onboarding();
        onboardingInstitution.setInstitution(inst);
        Onboarding onboardingAggregate = new Onboarding();
        onboardingAggregate.setInstitution(agg);

        when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(onboardingInstitution))
                .thenReturn(Collections.singletonList(onboardingAggregate));

        Onboarding onboarding = new Onboarding();
        when(onboardingMapper.mapToOnboarding(input)).thenReturn(onboarding);
        when(objectMapper.writeValueAsString(onboarding)).thenReturn("json");

        String result = completionServiceDefault.verifyOnboardingAggregate(input);
        assertTrue(result.contains("\"id\""));
        assertTrue(result.contains("\"productId\":\"product1\""));
        assertEquals("instId", input.getInstitution().getId());
        assertEquals("aggId", input.getAggregate().getId());
    }

    @Nested
    @TestProfile(CompletionServiceDefaultTest.ForceCreationProfile.class)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ForceCreationProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onboarding-functions.force-institution-persist", "true");
        }
    }

    @Test
    void forceInstitutionCreationFlagTrue() {
        // given
        Onboarding onboarding = createOnboarding();

        Institution institutionSa = new Institution();
        institutionSa.setTaxCode("taxCode");
        institutionSa.setInstitutionType(InstitutionType.SA);
        institutionSa.setOrigin(Origin.ANAC);
        onboarding.setInstitution(institutionSa);

        InstitutionsResponse institutionsResponse = new InstitutionsResponse();
        institutionsResponse.setInstitutions(List.of(dummyInstitutionResponse()));

        when(institutionApi.getInstitutionsUsingGET(any(), any(), any(), any(), any(), any()))
                .thenReturn(institutionsResponse);

        PanacheUpdate panacheUpdateMock = mock(PanacheUpdate.class);
        when(onboardingRepository.update("institution.id = ?1 and updatedAt = ?2 ", any(), any()))
                .thenReturn(panacheUpdateMock);

        // when
        completionServiceDefault.createInstitutionAndPersistInstitutionId(onboarding);

        // then
        verify(institutionApi, times(1)).getInstitutionsUsingGET(any(), any(), any(), any(), any(), any());
    }

    @Test
    void retrieveAggregates() {
        Onboarding onboarding = createOnboarding();
        DelegationWithPaginationResponse delegationWithPaginationResponse = new DelegationWithPaginationResponse();
        List<DelegationResponse> delegationResponses = new ArrayList<>();
        DelegationResponse delegationResponse = new DelegationResponse();
        delegationResponses.add(delegationResponse);
        delegationWithPaginationResponse.setDelegations(delegationResponses);
        when(delegationApi.getDelegationsUsingGET1(any(), eq(onboarding.getInstitution().getId()), eq(onboarding.getProductId()), any(), any(), any(), any(), any()))
                .thenReturn(delegationWithPaginationResponse);
        List<DelegationResponse> delegations = completionServiceDefault.retrieveAggregates(onboarding);

        assertEquals(delegationResponses, delegations);
        verify(delegationApi, times(1)).getDelegationsUsingGET1(any(), eq(onboarding.getInstitution().getId()), eq(onboarding.getProductId()), any(), any(), any(), any(), any());

    }

    private OnboardingAggregateOrchestratorInput buildInput(String institutionId, String aggregateId) {
        OnboardingAggregateOrchestratorInput input = new OnboardingAggregateOrchestratorInput();

        Institution inst = new Institution();
        inst.setId(institutionId);
        inst.setOriginId("originInst");
        inst.setTaxCode("taxInst");
        inst.setOrigin(Origin.IPA);

        Institution agg = new Institution();
        agg.setId(aggregateId);
        agg.setOriginId("originAgg");
        agg.setTaxCode("taxAgg");
        agg.setOrigin(Origin.IPA);

        input.setInstitution(inst);
        input.setAggregate(agg);
        input.setProductId("product1");
        return input;
    }

    private User createDummyUser(Onboarding onboarding) {
        User user = new User();
        user.setRole(PartyRole.MANAGER);
        user.setId("user-id");
        onboarding.setUsers(List.of(user));
        return user;
    }

    private InstitutionResponse dummyInstitutionResponse() {
        InstitutionResponse response = new InstitutionResponse();
        response.setId("response-id");
        return response;
    }

    private Onboarding createOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(onboarding.getId());
        onboarding.setProductId(productId);
        onboarding.setPricingPlan("pricingPlan");
        onboarding.setUsers(List.of());
        onboarding.setInstitution(new Institution());
        onboarding.setUserRequester(UserRequester.builder().userRequestUid("example-uid").build());

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("example");
        billing.setVatNumber("example");
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);
        return onboarding;
    }

    private Product createDummyProduct() {
        Product product = new Product();
        product.setInstitutionContractMappings(createDummyContractTemplateInstitution());
        product.setUserContractMappings(createDummyContractTemplateInstitution());
        product.setTitle("Title");
        product.setId(productId);
        return product;
    }

    private static Map<String, ContractTemplate> createDummyContractTemplateInstitution() {
        Map<String, ContractTemplate> institutionTemplate = new HashMap<>();
        ContractTemplate conctractTemplate = new ContractTemplate();
        conctractTemplate.setContractTemplatePath("example");
        conctractTemplate.setContractTemplateVersion("version");
        institutionTemplate.put(Product.CONTRACT_TYPE_DEFAULT, conctractTemplate);
        return institutionTemplate;
    }

}

