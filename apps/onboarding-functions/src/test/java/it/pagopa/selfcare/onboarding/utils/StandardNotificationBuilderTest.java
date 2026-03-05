package it.pagopa.selfcare.onboarding.utils;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Aggregator;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.model.AttributesResponse;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.onboarding.entity.Topic.SC_CONTRACTS;
import static it.pagopa.selfcare.onboarding.utils.NotificationBuilderTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class StandardNotificationBuilderTest {
    @InjectMock
    @RestClient
    InstitutionApi registryProxyInstitutionsApi;
    @InjectMock
    @RestClient
    GeographicTaxonomiesApi geographicTaxonomiesApi;
    @InjectMock
    @RestClient
    org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi;

    StandardNotificationBuilder standardNotificationBuilder;

    @BeforeEach
    public void setup() {
        NotificationConfig.Consumer consumer = mock(NotificationConfig.Consumer.class);
        when(consumer.topic()).thenReturn(SC_CONTRACTS.getValue());
        standardNotificationBuilder = new StandardNotificationBuilder("alternativeEmail", consumer, registryProxyInstitutionsApi, geographicTaxonomiesApi, coreInstitutionApi);
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setIsTest(Boolean.TRUE);
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);
        onboarding.setIsAggregator(true);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(token.getContractSigned(), notification.getFilePath());
        assertEquals(QueueEvent.ADD, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasNotActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        onboarding.setIsAggregator(false);
        Aggregator aggregator = new Aggregator();
        aggregator.setId("Id");
        aggregator.setDescription("Des");
        aggregator.setTaxCode("TaxCode");
        aggregator.setOriginId("OriginId");
        onboarding.setAggregator(aggregator);
        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getCreatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getCreatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.ADD, notification.getNotificationType());
        assertNotNull(notification.getRootAggregator());
        assertEquals("Id", notification.getRootAggregator().getInstitutionId());
        assertEquals("OriginId", notification.getRootAggregator().getOriginId());
        assertEquals("Des", notification.getRootAggregator().getDescription());
        assertFalse(notification.getIsAggregator());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasActivatedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingHasNotActivatedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:07:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingDeletedHasDeletedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.DELETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:06:00Z"), // updatedAt
                OffsetDateTime.parse("2020-12-02T18:22:00Z") // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertEquals("CLOSED", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getDeletedAt(), notification.getClosedAt().toLocalDateTime());
        assertEquals(onboarding.getDeletedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationToSendWhenOnboardingDeletedHasNotDeletedAtAndQueueEventUpdate() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.DELETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:06:00Z"), // updatedAt
                null // deletedAt
        );
        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);

        assertNotNull(notification);
        assertEquals("CLOSED", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(onboarding.getUpdatedAt(), notification.getClosedAt().toLocalDateTime());
        assertEquals(QueueEvent.UPDATE, notification.getNotificationType());
    }

    @Test
    void toNotificationAttributesNotNull() {

        // Create Institution
        InstitutionResponse institution = createInstitution();
        AttributesResponse attribute = new AttributesResponse();
        attribute.setCode("code");
        institution.setAttributes(List.of(attribute));
        // Create Token
        Token token = createToken();
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:00:00Z"), // updatedAt
                null
        );

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);


        //when
        NotificationToSend notificationToSend = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        //then
        assertNotNull(notificationToSend);
        verifyNoInteractions(registryProxyInstitutionsApi);
        verifyNoInteractions(geographicTaxonomiesApi);
    }

    @Test
    void toNotificationCityNull() {

        // Create Institution
        InstitutionResponse institution = createInstitution();
        institution.setCity(null);
        // Create Token
        Token token = createToken();
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:00:00Z"), // updatedAt
                null
        );
        onboarding.getInstitution().setInstitutionType(InstitutionType.PA);

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);

        mockPartyRegistryProxy(registryProxyInstitutionsApi, geographicTaxonomiesApi, institution);

        //when
        NotificationToSend notificationToSend = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        //then
        assertNotNull(notificationToSend);
        verify(registryProxyInstitutionsApi).findInstitutionUsingGET(any(), any(), any());
        verify(geographicTaxonomiesApi).retrieveGeoTaxonomiesByCodeUsingGET(any());

    }

    @Test
    void toNotificationGsp() {

        // Create Institution
        InstitutionResponse institution = createInstitution();
        // Create Token
        Token token = createToken();
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                null, // activatedAt
                OffsetDateTime.parse("2020-11-02T10:00:00Z"), // updatedAt
                null
        );
        onboarding.getInstitution().setInstitutionType(InstitutionType.GSP);
        onboarding.getInstitution().setOrigin(Origin.SELC);

        //when
        NotificationToSend notificationToSend = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        //then
        assertNotNull(notificationToSend);

    }

    @Test
    void toNotificationToSendWhenPSPOnboardingHasActivatedAtAndQueueEventAdd() {

        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        onboarding.getInstitution().getPaymentServiceProvider().setProviderNames(List.of("providerName1", "providerName2"));
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        // Create Institution
        InstitutionResponse institution = createInstitution();

        // Create Token
        Token token = createToken();

        InstitutionResponse institutionParentResource = new InstitutionResponse();
        institutionParentResource.setOriginId("parentOriginId");
        when(coreInstitutionApi.retrieveInstitutionByIdUsingGET(any(), any()))
                .thenReturn(institutionParentResource);
        onboarding.setIsAggregator(true);

        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());
        assertEquals(onboarding.getActivatedAt(), notification.getCreatedAt().toLocalDateTime());
        assertEquals(onboarding.getActivatedAt(), notification.getUpdatedAt().toLocalDateTime());
        assertEquals(token.getContractSigned(), notification.getFilePath());
        assertEquals(QueueEvent.ADD, notification.getNotificationType());
        assertEquals(2, notification.getInstitution().getPaymentServiceProvider().getProviderNames().size());
    }


    @Test
    void toSetTokenDataForCharWithAccent() {
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        onboarding.getInstitution().getPaymentServiceProvider().setProviderNames(List.of("providerName1", "providerName2"));
        onboarding.setWorkflowType(WorkflowType.CONFIRMATION);
        // Create Institution
        InstitutionResponse institution = createInstitution();

        // Create Token
        Token token = createToken();
        token.setContractSigned("path/Interoperabilità.p7m");


        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);
        standardNotificationBuilder.setTokenData(notification, token);

        assertNotNull(notification);
        assertNull(notification.getClosedAt());
        assertEquals("ACTIVE", notification.getState());
        assertEquals(TOKEN_ID, notification.getOnboardingTokenId());

    }

    @Test
    void buildNotificationToSendWithReferenceOnboardingId() {
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
        String referenceOnboardingId = "ref-onboarding-123";
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
        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        // then
        assertNotNull(notification);
        assertEquals(referenceOnboardingId, notification.getReferenceOnboardingId());
    }

    @Test
    void buildNotificationToSendWithoutReferenceOnboardingId() {
        // Create Onboarding
        Onboarding onboarding = createOnboarding(
                OnboardingStatus.COMPLETED,
                OffsetDateTime.parse("2020-11-01T10:00:00Z"), // createdAt
                OffsetDateTime.parse("2020-11-02T10:02:00Z"), // activatedAt
                OffsetDateTime.parse("2020-11-02T10:05:00Z"), // updatedAt
                null // deletedAt
        );
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
        NotificationToSend notification = standardNotificationBuilder.buildNotificationToSend(onboarding, token, institution, QueueEvent.ADD);

        // then
        assertNotNull(notification);
        assertNull(notification.getReferenceOnboardingId());
    }
}