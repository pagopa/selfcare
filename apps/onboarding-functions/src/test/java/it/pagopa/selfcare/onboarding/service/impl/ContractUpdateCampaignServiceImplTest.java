package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse.Outcome.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignRequest;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse;
import it.pagopa.selfcare.onboarding.dto.NotificationMailRequest;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.service.NotificationService;
import it.pagopa.selfcare.onboarding.service.OnboardingRepositoryService;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;

@QuarkusTest
class ContractUpdateCampaignServiceImplTest {

    private static final String INSTITUTION_ID = "institution-id";
    private static final String PEC = "ente@pec.it";

    @Inject
    ContractUpdateCampaignServiceImpl service;
    @Inject
    MailTemplatePathConfig templatePathConfig;
    @Inject
    MailTemplatePlaceholdersConfig templatePlaceholdersConfig;

    @InjectMock
    OnboardingRepositoryService onboardingRepositoryService;
    @InjectMock
    ProductService productService;
    @InjectMock
    NotificationService notificationService;
    @RestClient
    @InjectMock
    InstitutionApi institutionApi;

    @Test
    void send_shouldSendPecAndFlagAdhesion() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(adhesion("io-id", "prod-io", null)));
        mockInstitution(PEC);
        mockProduct("prod-io", "App IO");

        ContractUpdateCampaignResponse response = service.send(request("prod-io"));

        assertEquals(new ContractUpdateCampaignResponse(SENT, PEC), response);
        ArgumentCaptor<Document> queryCaptor = ArgumentCaptor.forClass(Document.class);
        verify(onboardingRepositoryService).findByQuery(queryCaptor.capture());
        assertEquals(new Document("institution.id", INSTITUTION_ID)
                .append("productId", new Document("$in", List.of("prod-io")))
                .append("status", "COMPLETED")
                .append("referenceOnboardingId", new Document("$exists", false)), queryCaptor.getValue());
        NotificationMailRequest mail = captureMail();
        assertEquals(List.of(PEC), mail.getDestinationMails());
        assertEquals(templatePathConfig.contractUpdateCampaignPath(), mail.getTemplatePath());
        assertEquals("App IO", mail.getMailParameters().get(templatePlaceholdersConfig.productName()));
        assertEquals(2, mail.getMailParameters().size());
        assertEquals(templatePlaceholdersConfig.completeSelfcarePlaceholder() + "/dashboard/" + INSTITUTION_ID + "/documents",
                mail.getMailParameters().get(ContractUpdateCampaignServiceImpl.DOCUMENTS_URL_PLACEHOLDER));
        verify(onboardingRepositoryService).setContractUpdateRequired(eq(List.of("io-id")), any());
    }

    @Test
    void send_shouldSendOnePecForBothProducts() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(
                adhesion("io-id", "prod-io", null), adhesion("pagopa-id", "prod-pagopa", null)));
        mockInstitution(PEC);
        mockProduct("prod-io", "App IO");
        mockProduct("prod-pagopa", "Piattaforma pagoPA");

        assertEquals(SENT, service.send(request("prod-io", "prod-pagopa")).outcome());

        verify(notificationService, times(1)).sendMail(any());
        assertEquals("App IO e Piattaforma pagoPA", captureMail().getMailParameters().get(templatePlaceholdersConfig.productName()));
        verify(onboardingRepositoryService).setContractUpdateRequired(eq(List.of("io-id", "pagopa-id")), any());
    }

    @Test
    void send_shouldNotifyOnlyAdhesionsNotYetNotified() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(
                adhesion("io-id", "prod-io", LocalDateTime.now()), adhesion("pagopa-id", "prod-pagopa", null)));
        mockInstitution(PEC);
        mockProduct("prod-pagopa", "Piattaforma pagoPA");

        assertEquals(SENT, service.send(request("prod-io", "prod-pagopa")).outcome());

        assertEquals("Piattaforma pagoPA", captureMail().getMailParameters().get(templatePlaceholdersConfig.productName()));
        verify(productService, never()).getProduct("prod-io");
        verify(onboardingRepositoryService).setContractUpdateRequired(eq(List.of("pagopa-id")), any());
    }

    @Test
    void send_shouldReturnAlreadyNotifiedWhenAllAdhesionsHaveRequiredAt() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(adhesion("io-id", "prod-io", LocalDateTime.now())));

        assertEquals(new ContractUpdateCampaignResponse(ALREADY_NOTIFIED, null), service.send(request("prod-io")));

        verifyNoInteractions(institutionApi, notificationService);
        verify(onboardingRepositoryService, never()).setContractUpdateRequired(anyList(), any());
    }

    @Test
    void send_shouldReturnInvalidWhenAnAdhesionIsMissing() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(adhesion("io-id", "prod-io", null)));

        assertEquals(new ContractUpdateCampaignResponse(INVALID, null), service.send(request("prod-io", "prod-pagopa")));

        verifyNoInteractions(institutionApi, notificationService);
        verify(onboardingRepositoryService, never()).setContractUpdateRequired(anyList(), any());
    }

    @Test
    void send_shouldReturnMissingPecWhenInstitutionHasNoDigitalAddress() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(adhesion("io-id", "prod-io", null)));
        mockInstitution(" ");

        assertEquals(new ContractUpdateCampaignResponse(MISSING_PEC, null), service.send(request("prod-io")));

        verifyNoInteractions(notificationService);
        verify(onboardingRepositoryService, never()).setContractUpdateRequired(anyList(), any());
    }

    @Test
    void send_shouldNotFlagAdhesionsWhenMailFails() {
        when(onboardingRepositoryService.findByQuery(any())).thenReturn(List.of(adhesion("io-id", "prod-io", null)));
        mockInstitution(PEC);
        mockProduct("prod-io", "App IO");
        doThrow(new GenericOnboardingException("mail error")).when(notificationService).sendMail(any());

        ContractUpdateCampaignRequest request = request("prod-io");
        assertThrows(GenericOnboardingException.class, () -> service.send(request));

        verify(onboardingRepositoryService, never()).setContractUpdateRequired(anyList(), any());
    }

    private static ContractUpdateCampaignRequest request(String... productIds) {
        return new ContractUpdateCampaignRequest(INSTITUTION_ID, List.of(productIds));
    }

    private static Onboarding adhesion(String id, String productId, LocalDateTime contractUpdateRequiredAt) {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(id);
        onboarding.setProductId(productId);
        onboarding.setContractUpdateRequiredAt(contractUpdateRequiredAt);
        return onboarding;
    }

    private void mockInstitution(String digitalAddress) {
        InstitutionResponse institution = new InstitutionResponse();
        institution.setDigitalAddress(digitalAddress);
        when(institutionApi.retrieveInstitutionByIdUsingGET(INSTITUTION_ID, null)).thenReturn(institution);
    }

    private void mockProduct(String productId, String title) {
        ProductResponse product = new ProductResponse();
        product.setTitle(title);
        when(productService.getProduct(productId)).thenReturn(product);
    }

    private NotificationMailRequest captureMail() {
        ArgumentCaptor<NotificationMailRequest> captor = ArgumentCaptor.forClass(NotificationMailRequest.class);
        verify(notificationService).sendMail(captor.capture());
        return captor.getValue();
    }
}
