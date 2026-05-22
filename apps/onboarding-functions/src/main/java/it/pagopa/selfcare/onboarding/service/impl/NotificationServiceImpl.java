package it.pagopa.selfcare.onboarding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.FileMailData;
import it.pagopa.selfcare.onboarding.dto.NotificationMailRequest;
import it.pagopa.selfcare.onboarding.dto.NotificationMailType;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.MailTemplate;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.service.ContractService;
import it.pagopa.selfcare.onboarding.service.NotificationService;
import it.pagopa.selfcare.product.entity.EmailTemplate;
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
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

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

    public NotificationServiceImpl(MailTemplatePlaceholdersConfig templatePlaceholdersConfig, MailTemplatePathConfig templatePathConfig,
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
    public String rejectOnboardingUrl() {
        return templatePlaceholdersConfig.rejectOnboardingUrlValue();
    }

    @Override
    public void sendMail(NotificationMailRequest notificationMailRequest) {
        log.info(
                "Preparing notification mail - type: {}, templatePath: {}, recipients: {}, hasAttachment: {}, prefixSubjectPresent: {}",
                notificationMailRequest.getType(),
                notificationMailRequest.getTemplatePath(),
                Optional.ofNullable(notificationMailRequest.getDestinationMails()).map(List::size).orElse(0),
                Objects.nonNull(notificationMailRequest.getFileMailData()),
                Objects.nonNull(notificationMailRequest.getPrefixSubject()));
        try {
            List<String> destinationMails = notificationMailRequest.getDestinationMails();
            String templateName = notificationMailRequest.getTemplatePath();
            Map<String, String> mailParameters = notificationMailRequest.getMailParameters();
            String prefixSubject = notificationMailRequest.getPrefixSubject();
            FileMailData fileMailData = notificationMailRequest.getFileMailData();

            if (destinationMailTest) {
                log.warn(
                        "Destination mail test mode enabled - overriding recipient list with test address: {}",
                        destinationMailTestAddress);
            }

            // Dev mode send mail to test digital address
            String destination = destinationMailTest
                    ? destinationMailTestAddress
                    : destinationMails.get(0);

            log.info(
                    "Loading mail template - templatePath: {}, destination: {}, prefixSubjectPresent: {}",
                    templateName,
                    destination,
                    Objects.nonNull(prefixSubject));
            String template = azureBlobClient.getFileAsText(templateName);
            MailTemplate mailTemplate = objectMapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);

            final String subject = Optional.ofNullable(prefixSubject).map(value -> String.format(FORMAT_STRING_MSG, value, mailTemplate.getSubject())).orElse(mailTemplate.getSubject());
            log.info(
                    "Mail template resolved - templatePath: {}, destination: {}, hasAttachment: {}, emailServiceAvailable: {}",
                    templateName,
                    destination,
                    Objects.nonNull(fileMailData),
                    isEmailServiceAvailable);

            Mail mail = Mail
                    .withHtml(destination, subject, html)
                    .setFrom(senderMail);

            if (Objects.nonNull(fileMailData)) {
                mail.addAttachment(fileMailData.getName(), fileMailData.getData(), fileMailData.getContentType());
            }

            send(mail);

            log.info(
                    "Mail send completed - destination: {}, subject: {}, templatePath: {}",
                    destination,
                    subject,
                    templateName);
        } catch (Exception e) {
            log.error(
                    "Mail send failed - templatePath: {}, errorType: {}, errorMessage: {}",
                    notificationMailRequest.getTemplatePath(),
                    e.getClass().getSimpleName(),
                    e.getMessage());
            throw new GenericOnboardingException(ERROR_DURING_SEND_MAIL.getMessage());
        }
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

        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.REGISTRATION)
                .destinationMails(List.of(destination))
                .templatePath(templatePathConfig.registrationRequestPath())
                .mailParameters(mailParameters)
                .prefixSubject(productName)
                .build());
    }

    @Override
    public void sendMailRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(
                institutionName,
                name,
                username,
                productName,
                onboardingId,
                templatePathConfig.registrationApprovePath(),
                NotificationMailType.REGISTRATION_APPROVE);
    }

    @Override
    public void sendMailOnboardingApprove(String institutionName, String name, String username, String productName, String onboardingId) {
        sendMailOnboardingOrRegistrationApprove(
                institutionName,
                name,
                username,
                productName,
                onboardingId,
                templatePathConfig.onboardingApprovePath(),
                NotificationMailType.ONBOARDING_APPROVE);
    }


    private void sendMailOnboardingOrRegistrationApprove(String institutionName, String name, String username, String productName, String onboardingId, String templatePath, NotificationMailType notificationMailType) {
        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.notificationRequesterSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        StringBuilder adminApproveLink = new StringBuilder(templatePlaceholdersConfig.adminLink());
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), adminApproveLink.append(onboardingId).toString());

        sendMail(NotificationMailRequest.builder()
                .type(notificationMailType)
                .destinationMails(List.of(notificationAdminMail))
                .templatePath(templatePath)
                .mailParameters(mailParameters)
                .prefixSubject(productName)
                .build());
    }

    private void sendMailRegistrationForContractWithResolvedTemplate(String onboardingId, String destination, String name, String username, String productName, String institutionName, String templatePath, String confirmTokenUrl, String expirationDate) {

        // Prepare data for email
        Map<String, String> mailParameters = new HashMap<>();
        mailParameters.put(templatePlaceholdersConfig.productName(), productName);
        Optional.ofNullable(name).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userName(), value));
        Optional.ofNullable(username).ifPresent(value -> mailParameters.put(templatePlaceholdersConfig.userSurname(), value));
        mailParameters.put(templatePlaceholdersConfig.rejectTokenName(), templatePlaceholdersConfig.rejectTokenPlaceholder() + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.confirmTokenName(), confirmTokenUrl + onboardingId);
        mailParameters.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        mailParameters.put(templatePlaceholdersConfig.expirationDate(), expirationDate);

        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.REGISTRATION_CONTRACT)
                .destinationMails(List.of(destination))
                .templatePath(templatePath)
                .mailParameters(mailParameters)
                .prefixSubject(productName)
                .build());
    }

    private void sendMailRegistrationForContractWithResolvedTemplate(SendMailInput sendMailInput, String confirmTokenUrl, String expirationDate, OnboardingWorkflow onboardingWorkflow) {
        // Prepare data for email
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        final String onboardingId = onboarding.getId();
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

        String templatePath = getTemplateMailPath(
                sendMailInput.getProduct(),
                onboardingWorkflow.getEmailRegistrationPath(templatePathConfig),
                onboarding,
                OnboardingStatus.REQUEST);
        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.REGISTRATION_CONTRACT)
                .destinationMails(List.of(onboarding.getInstitution().getDigitalAddress()))
                .templatePath(templatePath)
                .mailParameters(mailParameters)
                .prefixSubject(sendMailInput.getProduct().getTitle())
                .build());

    }

    @Override
    public void sendMailRegistrationForContract(SendMailInput sendMailInput, String expirationDate, OnboardingWorkflow onboardingWorkflow) {
        final String confirmTokenUrl = onboardingWorkflow.getConfirmTokenUrl(templatePlaceholdersConfig);
        sendMailRegistrationForContractWithResolvedTemplate(sendMailInput, confirmTokenUrl, expirationDate, onboardingWorkflow);
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

        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.REGISTRATION_AGGREGATOR)
                .destinationMails(List.of(destination))
                .templatePath(templatePathConfig.registrationAggregatorPath())
                .mailParameters(mailParameters)
                .prefixSubject(productName)
                .build());
    }

    @Override
    public void sendMailRegistrationForContract(String onboardingId, String destination, String name, String username, String productName, String institutionName, String expirationDate, OnboardingWorkflow onboardingWorkflow) {
        String templatePath = onboardingWorkflow.getEmailRegistrationPath(templatePathConfig);
        String confirmTokenUrl = onboardingWorkflow.getConfirmTokenUrl(templatePlaceholdersConfig);
        sendMailRegistrationForContractWithResolvedTemplate(onboardingId, destination, name, username, productName, institutionName, templatePath, confirmTokenUrl, expirationDate);
    }

    @Override
    public void sendCompletedEmail(List<String> destinationMails, Product product, OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        String templatePath = getTemplateMailPath(
                product,
                onboardingWorkflow.getEmailCompletionPath(templatePathConfig),
                onboarding,
                OnboardingStatus.COMPLETED);

        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.businessName(), onboarding.getInstitution().getDescription());
        mailParameter.put(templatePlaceholdersConfig.completeProductName(), product.getTitle());
        mailParameter.put(templatePlaceholdersConfig.completeSelfcareName(), templatePlaceholdersConfig.completeSelfcarePlaceholder());

        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.COMPLETED)
                .destinationMails(destinationMails)
                .templatePath(templatePath)
                .mailParameters(mailParameter)
                .prefixSubject(product.getTitle())
                .fileMailData(retrieveFileMetadataPagopaLogo())
                .build());
    }

    @Override
    public void sendDeletedEmail(List<String> destinationMails, Product product, Onboarding onboarding) {
        String templatePath = getTemplateMailPath(product, templatePathConfig.deletePath(), onboarding, OnboardingStatus.DELETED);
        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.completeProductName(), product.getTitle());
        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.DELETED)
                .destinationMails(destinationMails)
                .templatePath(templatePath)
                .mailParameters(mailParameter)
                .prefixSubject(product.getTitle())
                .fileMailData(retrieveFileMetadataPagopaLogo())
                .build());

    }

    @Override
    public void sendMailRejection(List<String> destinationMails, Product product, Onboarding onboarding) {
        String templatePath = getTemplateMailPath(product, templatePathConfig.rejectPath(), onboarding, OnboardingStatus.REJECTED);
        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.completeProductName(), product.getTitle());
        mailParameter.put(templatePlaceholdersConfig.reasonForReject(), onboarding.getReasonForReject());
        mailParameter.put(templatePlaceholdersConfig.rejectOnboardingUrlPlaceholder(), templatePlaceholdersConfig.rejectOnboardingUrlValue() + product.getId());
        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.REJECTION)
                .destinationMails(destinationMails)
                .templatePath(templatePath)
                .mailParameters(mailParameter)
                .prefixSubject(product.getTitle())
                .fileMailData(retrieveFileMetadataPagopaLogo())
                .build());
    }

    @Override
    public void sendCompletedEmailAggregate(String institutionName, List<String> destinationMails) {

        Map<String, String> mailParameter = new HashMap<>();
        mailParameter.put(templatePlaceholdersConfig.institutionDescription(), institutionName);
        mailParameter.put(templatePlaceholdersConfig.completeSelfcareName(), templatePlaceholdersConfig.completeSelfcarePlaceholder());

        sendMail(NotificationMailRequest.builder()
                .type(NotificationMailType.COMPLETED_AGGREGATE)
                .destinationMails(destinationMails)
                .templatePath(templatePathConfig.completePathAggregate())
                .mailParameters(mailParameter)
                .prefixSubject(null)
                .fileMailData(retrieveFileMetadataPagopaLogo())
                .build());
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

    private String getTemplateMailPath(Product product, String defaultTemplatePath, Onboarding onboarding, OnboardingStatus templateStatus) {
        log.info("Retrieving emailTemplate given institutionType {}, workflowType: {}, status: {}",
                onboarding.getInstitution().getInstitutionType().name(),
                onboarding.getWorkflowType().name(),
                templateStatus.name());
        logEmailTemplatesMap(product);
        Optional<EmailTemplate> emailTemplateOpt =
                product.getEmailTemplate(
                        onboarding.getInstitution().getInstitutionType().name(),
                        onboarding.getWorkflowType().name(),
                        templateStatus.name());
        if (emailTemplateOpt.isPresent()) {
            String emailTemplatePath = emailTemplateOpt.get().getPath();
            log.debug("Using custom email template path: {}", emailTemplatePath);
            return emailTemplatePath;
        } else {
            log.debug("Using default email template from config: {}", defaultTemplatePath);
            return defaultTemplatePath;
        }
    }

    private static void logEmailTemplatesMap(Product product) {
        // Log structured view of the email templates map to ease debugging
        Map<String, Map<String, List<EmailTemplate>>> templates = product.getEmailTemplates();
        if (templates == null) {
            log.info("Email templates map is null for product {}", product.getAlias());
        } else {
            templates.forEach(
                    (institutionType, workflowMap) -> {
                        log.info("Email templates institutionType {} workflowTypes {}",
                                institutionType,
                                workflowMap != null ? workflowMap.keySet() : null);
                        if (workflowMap != null) {
                            workflowMap.forEach(
                                    (workflowType, emailTemplates) -> {
                                        if (emailTemplates == null) {
                                            log.info("Email templates institutionType {} workflowType {}: none",
                                                    institutionType,
                                                    workflowType);
                                            return;
                                        }
                                        emailTemplates.forEach(
                                                template -> log.info(
                                                        "Email template entry institutionType {} workflowType {} status {} path {} version {}",
                                                        institutionType,
                                                        workflowType,
                                                        template.getStatus(),
                                                        template.getPath(),
                                                        template.getVersion()));
                                    });
                        }
                    });
        }
    }
}
