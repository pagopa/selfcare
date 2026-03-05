package it.pagopa.selfcare.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.FileMailData;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.MailTemplate;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.text.StringSubstitutor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static it.pagopa.selfcare.onboarding.utils.GenericError.ERROR_DURING_SEND_MAIL;


@ApplicationScoped
public class NotificationServiceDefault implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceDefault.class);

    public static final String FORMAT_STRING_MSG = "%s: %s";
    public static final String PAGOPA_LOGO_FILENAME = "pagopa-logo.png";
    private final MailTemplatePlaceholdersConfig templatePlaceholdersConfig;
    private final MailTemplatePathConfig templatePathConfig;
    private final AzureBlobClient azureBlobClient;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final String senderMail;
    private final boolean destinationMailTest;
    private final String destinationMailTestAddress;
    private final String notificationAdminMail;
    private final Mailer mailer;

    private final boolean isEmailServiceAvailable;

    public NotificationServiceDefault(MailTemplatePlaceholdersConfig templatePlaceholdersConfig, MailTemplatePathConfig templatePathConfig,
                                      AzureBlobClient azureBlobClient, ObjectMapper objectMapper, Mailer mailer, ContractService contractService,
                                      @ConfigProperty(name = "onboarding-functions.notification-admin-email") String notificationAdminMail,
                                      @ConfigProperty(name = "onboarding-functions.sender-mail") String senderMail,
                                      @ConfigProperty(name = "onboarding-functions.destination-mail-test") Boolean destinationMailTest,
                                      @ConfigProperty(name = "onboarding-functions.destination-mail-test-address") String destinationMailTestAddress,
                                      @ConfigProperty(name = "onboarding-functions.email.service.available") boolean isEmailServiceAvailable) {
        this.templatePlaceholdersConfig = templatePlaceholdersConfig;
        this.templatePathConfig = templatePathConfig;
        this.azureBlobClient = azureBlobClient;
        this.objectMapper = objectMapper;
        this.contractService = contractService;
        this.senderMail = senderMail;
        this.destinationMailTest = destinationMailTest;
        this.destinationMailTestAddress = destinationMailTestAddress;
        this.notificationAdminMail = notificationAdminMail;
        this.mailer = mailer;
        this.isEmailServiceAvailable = isEmailServiceAvailable;
    }

    @Override
    public void sendMailRegistration(String institutionName, String destination, String name, String username, String productName, String expirationDate) {

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        mailParameters.put(templatePlaceholdersConfig.expirationDate(), expirationDate);

        sendMailWithFile(List.of(destination), templatePathConfig.registrationRequestPath(), mailParameters, productName, null);
    }

    @Override
    public void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(institutionName, name, username, productName, onboardingId, templatePathConfig.registrationApprovePath());
    }

    @Override
    public void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(institutionName, name, username, productName, onboardingId, templatePathConfig.onboardingApprovePath());
    }


    private void sendMailOnboardingOrRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId, String templatePath) {
        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        StringBuilder adminApproveLink = new StringBuilder(templatePlaceholdersConfig.adminLink());
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), adminApproveLink.append(onboardingId).toString());

        sendMailWithFile(List.of(notificationAdminMail), templatePath, mailParameters, productName, null);
    }

    //TODO create an object to avoid multiple parameters in input
    @Override
    public void sendMailRegistrationForContract(String onboardingId, String destination, String name, String username, String productName, String institutionName, String templatePath, String confirmTokenUrl, String expirationDate) {

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), confirmTokenUrl + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        mailParameters.put(templatePlaceholdersConfig.expirationDate(), expirationDate);

        sendMailWithFile(List.of(destination), templatePath, mailParameters, productName, null);
    }

    @Override
    public void sendMailRegistrationForContract(String onboardingId, String destination, SendMailInput sendMailInput, String templatePath, String confirmTokenUrl, String expirationDate) {
        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), sendMailInput.getProduct().getTitle());
        Optional.ofNullable(sendMailInput.getUserRequestName()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(sendMailInput.getUserRequestSurname()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), confirmTokenUrl + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), sendMailInput.getInstitutionName());
        Optional.ofNullable(sendMailInput.getManagerName()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.managerName(), value));
        Optional.ofNullable(sendMailInput.getManagerSurname()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.managerSurname(), value));
        Optional.ofNullable(sendMailInput.getPreviousManagerName()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.previousManagerName(), value));
        Optional.ofNullable(sendMailInput.getPreviousManagerSurname()).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.previousManagerSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.expirationDate(), expirationDate);

        sendMailWithFile(List.of(destination), templatePath, mailParameters, sendMailInput.getProduct().getTitle(), null);

    }

    @Override
    public void sendMailRegistrationForContractAggregator(String onboardingId, String destination, String name, String username, String productName, String expirationDate) {

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), templatePlaceholdersConfig.confirmTokenPlaceholder() + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.expirationDate(), expirationDate);

        sendMailWithFile(List.of(destination), templatePathConfig.registrationAggregatorPath(), mailParameters, productName, null);
    }

    @Override
    public void sendCompletedEmail(String institutionName, List<String> destinationMails, Product product, InstitutionType institutionType, OnboardingWorkflow onboardingWorkflow) {
        String templatePath;
        Onboarding onboarding = onboardingWorkflow.getOnboarding();

        if (product.getEmailTemplate(institutionType.name(), onboarding.getWorkflowType().name(), onboarding.getStatus().name()).isPresent()) {
            templatePath = product.getEmailTemplate(institutionType.name(), onboarding.getWorkflowType().name(), onboarding.getStatus().name()).get().getPath();
        } else {
            templatePath = onboardingWorkflow.getEmailCompletionPath(templatePathConfig);
        }

        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.businessName(), institutionName);
        mailParameter.put(templatePlaceholdersConfig.completeProductName(), product.getTitle());
        mailParameter.put(templatePlaceholdersConfig.completeSelfcareName(), templatePlaceholdersConfig.completeSelfcarePlaceholder());

        sendMailWithFile(destinationMails, templatePath, mailParameter, product.getTitle(), retrieveFileMetadataPagopaLogo());
    }


    @Override
    public void sendMailRejection(List<String> destinationMails, Product product, String reasonForReject) {

        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.completeProductName(), product.getTitle());
        mailParameter.put(templatePlaceholdersConfig.reasonForReject(), reasonForReject);
        mailParameter.put(templatePlaceholdersConfig.rejectOnboardingUrlPlaceholder(), templatePlaceholdersConfig.rejectOnboardingUrlValue() + product.getId());
        sendMailWithFile(destinationMails, templatePathConfig.rejectPath(), mailParameter, product.getTitle(), retrieveFileMetadataPagopaLogo());
    }

    @Override
    public void sendCompletedEmailAggregate(String institutionName, List<String> destinationMails) {

        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        mailParameter.put(templatePlaceholdersConfig.completeSelfcareName(), templatePlaceholdersConfig.completeSelfcarePlaceholder());

        sendMailWithFile(destinationMails, templatePathConfig.completePathAggregate(), mailParameter, null, retrieveFileMetadataPagopaLogo());
    }

    private FileMailData retrieveFileMetadataPagopaLogo() {
        FileMailData fileMailData = null;
        Optional<File> optFileLogo = contractService.getLogoFile();
        if (optFileLogo.isPresent()) {
            fileMailData = new FileMailData();
            fileMailData.setContentType("image/png");
            fileMailData.setData(optFileLogo.map(File::toPath)
                    .map(path -> {
                        try {
                            return Files.readAllBytes(path);
                        } catch (IOException e) {
                            throw new GenericOnboardingException(e.getMessage());
                        }
                    })
                    .orElse(null));
            fileMailData.setName(PAGOPA_LOGO_FILENAME);
        }
        return fileMailData;
    }

    private void sendMailWithFile(List<String> destinationMail, String templateName, Map<String, String> mailParameters, String prefixSubject, FileMailData fileMailData) {
        try {

            // Dev mode send mail to test digital address
            String destination = destinationMailTest
                    ? destinationMailTestAddress
                    : destinationMail.get(0);

            log.info("Sending mail to {}, with prefixSubject {}", destination, prefixSubject);
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);

            final String subject = Optional.ofNullable(prefixSubject).map(value -> String.format(FORMAT_STRING_MSG, value, mailTemplate.getSubject())).orElse(mailTemplate.getSubject());

            Mail mail = Mail
                    .withHtml(destination, subject, html)
                    .setFrom(senderMail);

            if (Objects.nonNull(fileMailData)) {
                mail.addAttachment(fileMailData.getName(), fileMailData.getData(), fileMailData.getContentType());
            }

            send(mail);

            log.info("End of sending mail to {}, with subject {}", destination, subject);
        } catch (Exception e) {
            log.error(String.format(FORMAT_STRING_MSG, ERROR_DURING_SEND_MAIL, e.getMessage()));
            throw new GenericOnboardingException(ERROR_DURING_SEND_MAIL.getMessage());
        }
    }

    private void send(Mail mail) {
        if (isEmailServiceAvailable) {
            mailer.send(mail);
        }
    }



    @Override
    public void sendTestEmail(ExecutionContext context) {
        try {
            context.getLogger().info("Sending Test email to " + senderMail);
            String html = "TEST EMAIL";
            Mail mail = Mail
                    .withHtml(senderMail, html, html)
                    .setFrom(senderMail);

            send(mail);
            context.getLogger().info("End of sending mail to {}, with subject " + senderMail + " with subject " + mail);
        } catch (Exception e) {
            context.getLogger().severe(String.format(FORMAT_STRING_MSG, ERROR_DURING_SEND_MAIL, e.getMessage()));
            throw new GenericOnboardingException(ERROR_DURING_SEND_MAIL.getMessage());
        }
    }
}
