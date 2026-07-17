package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.client.webhook.WebhookRestClient;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.webhook.NotificationRequest;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.UserRequester;
import it.pagopa.selfcare.onboarding.service.impl.NotificationEventServiceImpl;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilder;
import it.pagopa.selfcare.onboarding.utils.NotificationBuilderFactory;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.document_json.model.RelatedDocumentResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NotificationEventServiceAttachmentsTest.AttachmentsEnabledProfile.class)
class NotificationEventServiceAttachmentsTest {

    @Inject
    NotificationEventServiceImpl notificationEventService;

    @InjectMock
    ProductService productService;

    @InjectMock
    NotificationBuilderFactory notificationBuilderFactory;

    @InjectMock
    DocumentService documentService;

    @InjectMock
    @RestClient
    EventHubRestClient eventHubRestClient;

    @InjectMock
    @RestClient
    WebhookRestClient webhookRestClient;

    @InjectMock
    @RestClient
    InstitutionApi institutionApi;

    @Test
    void send_shouldAddAttachmentsOnlyToQueuePayload_whenFeatureIsEnabled() {
        // given
        Onboarding onboarding = new Onboarding();
        onboarding.setId("onboarding-id");
        onboarding.setProductId("product-id");
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
        onboarding.setInstitution(new Institution());
        onboarding.setUsers(List.of());
        onboarding.setUserRequester(UserRequester.builder().userRequestUid("fake-user-id").build());

        Product product = new Product();
        product.setConsumers(List.of("Standard"));
        when(productService.getProduct("product-id")).thenReturn(product);
        when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
        when(documentService.getDocumentByOnboardingIdOrNull("onboarding-id")).thenReturn(new DocumentResponse());

        NotificationBuilder notificationBuilder = mock(NotificationBuilder.class);
        when(notificationBuilderFactory.create(any())).thenReturn(notificationBuilder);
        when(notificationBuilder.shouldSendNotification(any(), any())).thenReturn(true);
        when(notificationBuilder.buildNotificationToSend(any(), any(), any(), any()))
                .thenReturn(new NotificationToSend());

        RelatedDocumentResponse attachment = new RelatedDocumentResponse();
        attachment.setId("document-id");
        attachment.setFileName("attachment.pdf");
        attachment.setFilePath("/contracts/onboarding-id/attachments/attachment.pdf");
        attachment.setType(RelatedDocumentResponse.TypeEnum.ATTACHMENT);
        attachment.setMimeType("application/pdf");
        attachment.setCreatedAt(LocalDateTime.parse("2026-07-15T10:00:00"));
        when(documentService.getRelatedDocuments("onboarding-id")).thenReturn(List.of(attachment));

        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
        doNothing().when(webhookRestClient).sendNotification(any(NotificationRequest.class));

        // when
        notificationEventService.send(context, onboarding, QueueEvent.ADD);

        // then
        ArgumentCaptor<String> queuePayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventHubRestClient).sendMessage(eq("SC-Contracts"), queuePayloadCaptor.capture());
        String queuePayload = queuePayloadCaptor.getValue();
        assertTrue(queuePayload.contains("\"relatedDocuments\":["));
        assertTrue(queuePayload.contains("\"type\":\"ATTACHMENT\""));
        assertTrue(queuePayload.contains("\"mimeType\":\"application/pdf\""));
        assertTrue(queuePayload.contains("\"createdAt\":\"2026-07-15T10:00:00.000Z\""));

        ArgumentCaptor<NotificationRequest> webhookCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(webhookRestClient).sendNotification(webhookCaptor.capture());
        assertFalse(webhookCaptor.getValue().getPayload().contains("\"relatedDocuments\""));
        verify(documentService).getRelatedDocuments("onboarding-id");
    }

    public static class AttachmentsEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("notification.consumers.standard.include-related-documents", "true");
        }
    }
}
