package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS_FD;
import static it.pagopa.selfcare.onboarding.utils.NotificationBuilderTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class FdNotificationBuilderTest {
    @InjectMock
    @RestClient
    InstitutionApi registryProxyInstitutionsApi;
    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @InjectMock
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;
    @RestClient
    @InjectMock
    org.openapi.quarkus.user_json.api.UserApi userApi;

    FdNotificationBuilder fdNotificationBuilder;

    @BeforeEach
    public void setup() {
        NotificationConfig.Consumer consumer = mock(NotificationConfig.Consumer.class);
        when(consumer.topic()).thenReturn(SC_CONTRACTS_FD.getValue());
        fdNotificationBuilder = new FdNotificationBuilder("alternativeEmail", consumer, registryProxyInstitutionsApi, geographicTaxonomiesApi, coreInstitutionApi);
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = getOnboardingTest();
        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = fdNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNotEquals(onboarding.getId(), notification.getId());
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertNull(notification.getNotificationType());
        assertEquals(NotificationType.ADD_INSTITUTE, notification.getType());
        assertNull(notification.getBilling().getTaxCodeInvoicing());
        assertNull(notification.getBilling().isPublicServices());
        assertEquals(onboarding.getBilling().isPublicServices(), notification.getBilling().isPublicService());
        assertNull(notification.getFilePath());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventUserActive() {

        // Create Onboarding
        Onboarding onboarding = getOnboardingTest();

        Billing billing = new Billing();
        billing.setTaxCodeInvoicing("taxCodeInvoicing");
        onboarding.setBilling(billing);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        OnboardedProductResponse productResponse = getOnboardedProductResponse();

        UserResponse userResponse = new UserResponse();
        userResponse.id("userId1");


        UserDataResponse userDataResponse = getUserDataResponse(institution, productResponse, userResponse);

        List<UserDataResponse> users = new ArrayList<>();
        users.add(userDataResponse);


        when(userApi.retrieveUsers(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(users);

        NotificationUserToSend notification = fdNotificationBuilder.buildUserNotificationToSend(
                onboarding,
                token,
                institution,
                productResponse.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                productResponse.getUpdatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "ACTIVE",
                userDataResponse.getUserId(),
                productResponse.getRole(),
                productResponse.getProductRole());

        assertNotNull(notification);
        assertNotEquals(onboarding.getId(), notification.getId());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(productResponse.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), notification.getCreatedAt());
        assertEquals(productResponse.getUpdatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), notification.getUpdatedAt());
        assertEquals(NotificationUserType.ACTIVE_USER, notification.getType());
        assertEquals(productResponse.getRole(), notification.getUser().getRole());

        assertTrue(notification.toString().contains("institutionId='" + notification.getInstitutionId() + "'"));

    }

    private static UserDataResponse getUserDataResponse(InstitutionResponse institution, OnboardedProductResponse productResponse, UserResponse userResponse) {
        UserDataResponse userDataResponse = new UserDataResponse();
        userDataResponse.setId("userDataId1");
        userDataResponse.setUserId("userId1");
        userDataResponse.setInstitutionId(institution.getId());
        userDataResponse.setInstitutionDescription(institution.getDescription());
        userDataResponse.setUserMailUuid("usermail1");
        userDataResponse.setRole("OPERATOR");
        userDataResponse.setStatus("ACTIVE");
        userDataResponse.setProducts(List.of(productResponse));
        userDataResponse.setUserResponse(userResponse);
        return userDataResponse;
    }

    private static OnboardedProductResponse getOnboardedProductResponse() {
        OnboardedProductResponse productResponse = new OnboardedProductResponse();
        productResponse.setProductId(PRODUCT_ID);
        productResponse.setStatus(OnboardedProductState.ACTIVE);
        productResponse.setProductRole("security");
        productResponse.setRole("OPERATOR");
        productResponse.setEnv(Env.PROD);
        productResponse.setCreatedAt(OffsetDateTime.parse("2020-11-01T10:00:00Z"));
        productResponse.setUpdatedAt(OffsetDateTime.parse("2020-11-02T10:05:00Z"));
        return productResponse;
    }

    @Test
    void buildNotificationToSendWithReferenceOnboardingId() {
        // Create Onboarding
        Onboarding onboarding = getOnboardingTest();
        String referenceOnboardingId = "ref-onboarding-456";
        onboarding.setReferenceOnboardingId(referenceOnboardingId);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        // when
        NotificationToSend notification = fdNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        // then
        assertNotNull(notification);
        assertEquals(referenceOnboardingId, notification.getReferenceOnboardingId());
    }

    @Test
    void buildNotificationToSendWithoutReferenceOnboardingId() {
        // Create Onboarding
        Onboarding onboarding = getOnboardingTest();
        onboarding.setReferenceOnboardingId(null);

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        // when
        NotificationToSend notification = fdNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        // then
        assertNotNull(notification);
        assertNull(notification.getReferenceOnboardingId());
    }

    private static Onboarding getOnboardingTest() {
        return createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
    }
}