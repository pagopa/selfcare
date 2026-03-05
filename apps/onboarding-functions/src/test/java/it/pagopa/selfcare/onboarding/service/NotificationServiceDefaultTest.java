package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.product.entity.EmailTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class NotificationServiceDefaultTest {

    @InjectMock
    AzureBlobClient azureBlobClient;
    @InjectMock
    ContractService contractService;
    @Inject
    MailTemplatePlaceholdersConfig templatePlaceholdersConfig;
    @Inject
    MailTemplatePathConfig templatePathConfig;
    @Inject
    ObjectMapper objectMapper;
    Mailer mailer;
    NotificationServiceDefault notificationService;

    final String notificationAdminMail = "adminAddress";

    @BeforeEach
    void startup() {
        mailer = mock(Mailer.class);
        this.notificationService = new NotificationServiceDefault(templatePlaceholdersConfig, templatePathConfig,
                azureBlobClient, objectMapper, mailer, contractService, notificationAdminMail, "senderMail", false, "destinationMailTestAddress", true);
    }

    @Test
    void sendMailRegistrationWithContract() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";
        final String productName = "productName";

        when(azureBlobClient.getFileAsText(templatePathConfig.registrationPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistrationForContract(onboardingId, destination,"","", productName, "description", "contracts/template/mail/onboarding-request/1.0.1.json", "default", "30");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendMailRegistrationWithContractAggregator() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";
        final String productName = "productName";

        when(azureBlobClient.getFileAsText(templatePathConfig.registrationAggregatorPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistrationForContractAggregator(onboardingId, destination,"","", productName, "30");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendMailRegistrationWithContract_shouldThrowException() {
        final String onboardingId = "onboardingId";
        assertThrows(RuntimeException.class, () -> notificationService.sendMailRegistrationForContract(onboardingId,  "example@pagopa.it","mario","rossi","prod-example", "", "", "", "30"));
    }

    @Test
    void sendMailRegistrationWithContractAggregator_shouldThrowException() {
        final String onboardingId = "onboardingId";
        assertThrows(RuntimeException.class, () -> notificationService.sendMailRegistrationForContractAggregator(onboardingId,  "example@pagopa.it","mario","rossi","prod-example", "30"));
    }

    @Test
    void sendMailRegistration() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";

        when(azureBlobClient.getFileAsText(templatePathConfig.registrationRequestPath()))
                .thenReturn(mailTemplate);

        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistration(onboardingId, destination,"","","", "30");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendCompletedEmail() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";
        final String destination = "test@test.it";
        Product product = new Product();
        product.setTitle("productName");
        product.setId("prod-id");

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        when(azureBlobClient.getFileAsText(anyString()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PT);
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institution);
        onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
        onboarding.setStatus(OnboardingStatus.PENDING);
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution(onboarding, OnboardingWorkflowType.INSTITUTION.name());

        notificationService.sendCompletedEmail(institutionName, List.of(destination), product, InstitutionType.PA, onboardingWorkflow);

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendCompletedEmailRecoveringTemplateFromProduct() {
        // Arrange
        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";
        final String destination = "test@test.it";

        // Mock EmailTemplate
        EmailTemplate emailTemplate = new EmailTemplate();
        emailTemplate.setPath(mailTemplate);

        // Mock Product
        Product product = Mockito.mock(Product.class);
        when(product.getTitle()).thenReturn("productName");
        when(product.getEmailTemplate(
                eq(InstitutionType.PA.name()),
                eq(WorkflowType.IMPORT.name()),
                eq(OnboardingStatus.PENDING.name()))
        ).thenReturn(Optional.of(emailTemplate));

        // Mock file
        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());
        when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        // Mock mail template loading
        when(azureBlobClient.getFileAsText(anyString())).thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        // Mock onboarding workflow
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PA);
        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institution);
        onboarding.setWorkflowType(WorkflowType.IMPORT);
        onboarding.setStatus(OnboardingStatus.PENDING);
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution(onboarding, OnboardingWorkflowType.INSTITUTION.name());

        // Act
        notificationService.sendCompletedEmail(institutionName, List.of(destination), product, InstitutionType.PA, onboardingWorkflow);

        // Assert
        Mockito.verify(azureBlobClient, Mockito.times(1)).getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1)).send(mailArgumentCaptor.capture());

        Mail capturedMail = mailArgumentCaptor.getValue();
        assertEquals(destination, capturedMail.getTo().get(0));
    }

    @Test
    void sendMailRejection() {
        String reasonForReject = "string";
        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String destination = "test@test.it";
        Product product = new Product();
        product.setTitle("productName");
        product.setId("prod-id");

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        when(azureBlobClient.getFileAsText(templatePathConfig.rejectPath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRejection(List.of(destination), product, reasonForReject);

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendCompletedEmailAggregate() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";
        final String destination = "test@test.it";

        final File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("application.properties")).getFile());

        when(contractService.getLogoFile()).thenReturn(Optional.of(file));

        when(azureBlobClient.getFileAsText(templatePathConfig.completePathAggregate()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendCompletedEmailAggregate(institutionName, List.of(destination));

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }


    @Test
    void sendMailRegistrationApprove() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";

        when(azureBlobClient.getFileAsText(templatePathConfig.registrationApprovePath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailRegistrationApprove(institutionName, "name","username","product","token");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(notificationAdminMail, mailArgumentCaptor.getValue().getTo().get(0));
    }


    @Test
    void sendMailOnboardingApprove() {

        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";
        final String institutionName = "institutionName";

        when(azureBlobClient.getFileAsText(templatePathConfig.onboardingApprovePath()))
                .thenReturn(mailTemplate);
        Mockito.doNothing().when(mailer).send(any());

        notificationService.sendMailOnboardingApprove(institutionName, "name","username","product","token");

        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(notificationAdminMail, mailArgumentCaptor.getValue().getTo().get(0));
    }

    @Test
    void sendMailRegistrationForContractTest() {
        // given
        final String mailTemplate = "{\"subject\":\"example\",\"body\":\"example\"}";

        final String onboardingId = "onboardingId";
        final String destination = "test@test.it";

        when(azureBlobClient.getFileAsText(any()))
                .thenReturn(mailTemplate);

        Mockito.doNothing().when(mailer).send(any());
        SendMailInput sendMailInput = new SendMailInput();
        sendMailInput.setProduct(new Product());

        // when
        notificationService.sendMailRegistrationForContract(onboardingId, destination, sendMailInput, "test-path", "test-confirmUrl", "30");

        // then
        Mockito.verify(azureBlobClient, Mockito.times(1))
                .getFileAsText(any());

        ArgumentCaptor<Mail> mailArgumentCaptor = ArgumentCaptor.forClass(Mail.class);
        Mockito.verify(mailer, Mockito.times(1))
                .send(mailArgumentCaptor.capture());
        assertEquals(destination, mailArgumentCaptor.getValue().getTo().get(0));
    }

}
