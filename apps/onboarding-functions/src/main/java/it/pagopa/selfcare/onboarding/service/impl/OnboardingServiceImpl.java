package it.pagopa.selfcare.onboarding.service.impl;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.ManagingInstitutionSendEmail;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.AttachmentPdfRequestMapper;
import it.pagopa.selfcare.onboarding.mapper.ContractPdfRequestMapper;
import it.pagopa.selfcare.onboarding.mapper.DocumentBuilderRequestMapper;
import it.pagopa.selfcare.onboarding.service.*;
import it.pagopa.selfcare.onboarding.utils.GenericError;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.pagopa.selfcare.onboarding.utils.Utils.NOT_ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS;

@ApplicationScoped
public class OnboardingServiceImpl implements OnboardingService {

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";
    public static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USER_REQUEST_DOES_NOT_FOUND = "User request does not found for onboarding %s";
    public static final String ACTIVATED_AT_FIELD = "activatedAt";
    public static final String DELETED_AT_FIELD = "deletedAt";
    private static final Logger log = LoggerFactory.getLogger(OnboardingServiceImpl.class);
    private static final String WORKFLOW_TYPE = "workflowType";

    private final NotificationService notificationService;
    private final DocumentService documentService;
    private final ProductService productService;
    private final PdvUserRegistryService pdvUserRegistryService;
    private final UserInstitutionRestService userInstitutionRestService;
    private final UserNotificationService userNotificationService;
    private final InfocamereService infocamereService;
    private final OnboardingRepositoryService onboardingRepositoryService;

    private final MailTemplatePathConfig mailTemplatePathConfig;
    private final MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;
    private final ContractPdfRequestMapper contractPdfRequestMapper;
    private final AttachmentPdfRequestMapper attachmentPdfRequestMapper;
    private final DocumentBuilderRequestMapper documentBuilderRequestMapper;

    public OnboardingServiceImpl(
            ProductService productService,
            MailTemplatePathConfig mailTemplatePathConfig,
            MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig,
            NotificationService notificationService,
            DocumentService documentService,
            ContractPdfRequestMapper contractPdfRequestMapper,
            AttachmentPdfRequestMapper attachmentPdfRequestMapper,
            DocumentBuilderRequestMapper documentBuilderRequestMapper,
            OnboardingRepositoryService onboardingRepositoryService,
            PdvUserRegistryService pdvUserRegistryService,
            UserInstitutionRestService userInstitutionRestService,
            UserNotificationService userNotificationService,
            InfocamereService infocamereService) {
        this.productService = productService;
        this.notificationService = notificationService;
        this.documentService = documentService;
        this.mailTemplatePathConfig = mailTemplatePathConfig;
        this.mailTemplatePlaceholdersConfig = mailTemplatePlaceholdersConfig;
        this.contractPdfRequestMapper = contractPdfRequestMapper;
        this.attachmentPdfRequestMapper = attachmentPdfRequestMapper;
        this.documentBuilderRequestMapper = documentBuilderRequestMapper;
        this.onboardingRepositoryService = onboardingRepositoryService;
        this.pdvUserRegistryService = pdvUserRegistryService;
        this.userInstitutionRestService = userInstitutionRestService;
        this.userNotificationService = userNotificationService;
        this.infocamereService = infocamereService;
    }

    public Optional<Onboarding> getOnboarding(String onboardingId) {
        return onboardingRepositoryService.findById(onboardingId);
    }

    public void createContract(OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();

        List<UserResource> delegates =
                onboarding.getUsers().stream()
                        .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
                        .map(
                                userToOnboard ->
                                        pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, userToOnboard.getId()))
                        .toList();
        UserResource manager = getUserResource(onboarding);
        Product product = productService.getProductIsValid(onboarding.getProductId());
        ContractPdfRequest request =
                contractPdfRequestMapper.toRequest(
                        onboarding,
                        manager,
                        delegates,
                        product,
                        onboardingWorkflow.getContractTemplatePath(product),
                        mailTemplatePlaceholdersConfig.rejectOnboardingUrlValue());

        documentService.createContractPdf(request);
    }

    private UserResource getUserResource(Onboarding onboarding) {
        String validManagerId = getValidManagerId(onboarding.getUsers());
        return pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, validManagerId);
    }

    public void createAttachment(OnboardingAttachment onboardingAttachment) {
        Onboarding onboarding = onboardingAttachment.getOnboarding();
        Product product = productService.getProductIsValid(onboarding.getProductId());
        AttachmentTemplate attachment = onboardingAttachment.getAttachment();
        AttachmentPdfRequest request =
                attachmentPdfRequestMapper.toRequest(
                        onboarding, attachment, product, getUserResource(onboarding));

        documentService.createAttachmentPdf(request);
    }

    public void saveTokenWithContract(OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        Product product = productService.getProductIsValid(onboarding.getProductId());
        DocumentBuilderRequest request =
                documentBuilderRequestMapper.toRequest(onboarding, product, onboardingWorkflow);
        documentService.saveDocument(request);
    }

    public void saveTokenWithAttachment(OnboardingAttachment onboardingAttachment) {
        Onboarding onboarding = onboardingAttachment.getOnboarding();
        Product product = productService.getProductIsValid(onboarding.getProductId());
        AttachmentTemplate attachmentTemplate = onboardingAttachment.getAttachment();
        DocumentBuilderRequest request =
                documentBuilderRequestMapper.toRequest(onboarding, product, attachmentTemplate, DocumentType.ATTACHMENT);
        documentService.saveDocument(request);
    }

    public void sendMailRegistration(Onboarding onboarding) {
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        String expirationDate = productService.getProductExpirationDate(onboarding.getProductId()).toString();

        notificationService.sendMailRegistration(
                onboarding.getInstitution().getDescription(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.getUserRequestName(),
                sendMailInput.getUserRequestSurname(),
                sendMailInput.getProduct().getTitle(),
                expirationDate);
    }

    public void sendMailRegistrationForUser(Onboarding onboarding) {

        log.info("Sending mail to user");
        User user = onboarding.getUsers().get(0);
        SendMailDto sendMailDto = new SendMailDto();
        sendMailDto.setInstitutionName(onboarding.getInstitution().getDescription());
        sendMailDto.setProductId(onboarding.getProductId());
        sendMailDto.setUserMailUuid(user.getUserMailUuid());

        userNotificationService.sendMailRequest(user.getId(), sendMailDto);
    }

    public void sendMailRegistrationForUserRequester(Onboarding onboarding) {

        log.info("Sending mail to user requester");
        UserRequester userRequester = onboarding.getUserRequester();
        SendMailDto sendMailDto = new SendMailDto();
        sendMailDto.setInstitutionName(onboarding.getInstitution().getDescription());
        sendMailDto.setProductId(onboarding.getProductId());
        sendMailDto.setUserMailUuid(userRequester.getUserMailUuid());

        userNotificationService.sendMailRequest(userRequester.getUserRequestUid(), sendMailDto);
    }

    public void sendMailManagingInstitution(ManagingInstitutionSendEmail request) {
        log.info("Sending mail to managing institution");
        SendMailDto sendMailDto = new SendMailDto();
        sendMailDto.setInstitutionName(request.getManagingInstitutionDescription());
        sendMailDto.setProductId(request.getProductId());
        sendMailDto.setUserMailUuid(request.getUserMailUuid());
        userNotificationService.sendMailRequest(request.getUserMailUuid(), sendMailDto);
    }

    public void saveVisuraForMerchant(Onboarding onboarding) {
        var taxCode = onboarding.getInstitution().getTaxCode();
        var bytes = infocamereService.getInstitutionVisuraByTaxCode(taxCode);
        final String filename = String.format("VISURA_%s.xml", taxCode);
        org.openapi.quarkus.document_json.api.DocumentContentControllerApi.SaveVisuraForMerchantMultipartForm request =
                new org.openapi.quarkus.document_json.api.DocumentContentControllerApi
                        .SaveVisuraForMerchantMultipartForm();
        request.fileContent = new ByteArrayInputStream(bytes);
        request.onboardingId = onboarding.getId();
        request.filename = filename;
        documentService.saveVisuraForMerchant(request);
    }

    public void sendMailRegistrationForContract(OnboardingWorkflow onboardingWorkflow) {

        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        final String templatePath = onboardingWorkflow.getEmailRegistrationPath(mailTemplatePathConfig);
        final String confirmTokenUrl =
                onboardingWorkflow.getConfirmTokenUrl(mailTemplatePlaceholdersConfig);

        String expirationDate = productService.getProductExpirationDate(onboarding.getProductId()).toString();

        notificationService.sendMailRegistrationForContract(
                onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput,
                templatePath,
                confirmTokenUrl, expirationDate);
    }

    public void sendMailRegistrationForContractAggregator(Onboarding onboarding) {
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);

        String expirationDate = productService.getProductExpirationDate(onboarding.getProductId()).toString();

        notificationService.sendMailRegistrationForContractAggregator(
                onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                sendMailInput.getUserRequestName(),
                sendMailInput.getUserRequestSurname(),
                sendMailInput.getProduct().getTitle(), expirationDate);
    }

    public void sendMailRegistrationForContractWhenApprove(OnboardingWorkflow onboardingWorkflow) {
        Onboarding onboarding = onboardingWorkflow.getOnboarding();
        Product product = productService.getProduct(onboarding.getProductId());

        String expirationDate = productService.getProductExpirationDate(onboarding.getProductId()).toString();

        notificationService.sendMailRegistrationForContract(
                onboarding.getId(),
                onboarding.getInstitution().getDigitalAddress(),
                onboarding.getInstitution().getDescription(),
                "",
                product.getTitle(),
                "description",
                onboardingWorkflow.getEmailRegistrationPath(mailTemplatePathConfig),
                onboardingWorkflow.getConfirmTokenUrl(mailTemplatePlaceholdersConfig), expirationDate);
    }

    public void sendMailRegistrationApprove(Onboarding onboarding) {
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);
        notificationService.sendMailRegistrationApprove(
                onboarding.getInstitution().getDescription(),
                sendMailInput.getUserRequestName(),
                sendMailInput.getUserRequestSurname(),
                sendMailInput.getProduct().getTitle(),
                onboarding.getId());
    }

    public void updateOnboardingExpiringDate(Onboarding onboarding) {
        Integer onboardingExpirationDays = productService.getProductExpirationDate(onboarding.getProductId());
        onboarding.setExpiringDate(OffsetDateTime.now().plusDays(onboardingExpirationDays).toLocalDateTime());
        onboardingRepositoryService.update(onboarding);
    }

    public void sendMailOnboardingApprove(Onboarding onboarding) {
        SendMailInput sendMailInput = builderWithProductAndUserRequest(onboarding);
        notificationService.sendMailOnboardingApprove(
                onboarding.getInstitution().getDescription(),
                sendMailInput.getUserRequestName(),
                sendMailInput.getUserRequestSurname(),
                sendMailInput.getProduct().getTitle(),
                onboarding.getId());
    }

    public String getValidManagerId(List<User> users) {
        log.debug("START - getOnboardingValidManager for users list size: {}", users.size());

        return users.stream()
                .filter(userToOnboard -> PartyRole.MANAGER == userToOnboard.getRole())
                .map(User::getId)
                .findAny()
                .orElseThrow(
                        () ->
                                new GenericOnboardingException(
                                        GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getMessage(),
                                        GenericError.MANAGER_NOT_FOUND_GENERIC_ERROR.getCode()));
    }

    private SendMailInput builderWithProductAndUserRequest(Onboarding onboarding) {
        SendMailInput sendMailInput = new SendMailInput();
        sendMailInput.setProduct(productService.getProduct(onboarding.getProductId()));

        // Set data of previousManager in case of workflowType USERS
        if (Objects.nonNull(onboarding.getPreviousManagerId())) {
            setManagerData(onboarding, sendMailInput);
        }

        // Retrieve user request name and surname
        UserResource userRequest =
                Optional.ofNullable(
                                pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
                        .orElseThrow(
                                () ->
                                        new GenericOnboardingException(
                                                String.format(USER_REQUEST_DOES_NOT_FOUND, onboarding.getId())));
        sendMailInput.setUserRequestName(Optional.ofNullable(userRequest.getName())
                .map(CertifiableFieldResourceOfstring::getValue)
                .orElse(""));
        sendMailInput.setUserRequestSurname(Optional.ofNullable(userRequest.getFamilyName())
                .map(CertifiableFieldResourceOfstring::getValue)
                .orElse(""));
        sendMailInput.setInstitutionName(Optional.ofNullable(onboarding.getInstitution().getDescription()).orElse(""));
        return sendMailInput;
    }

    public void updateOnboardingStatus(String onboardingId, OnboardingStatus status) {
        onboardingRepositoryService.updateStatus(onboardingId, status.name(), LocalDateTime.now());
    }

    public void updateOnboardingStatusAndInstanceId(
            String onboardingId, OnboardingStatus status, String instanceId) {
        onboardingRepositoryService.updateStatusAndInstanceId(
                onboardingId, status.name(), instanceId, LocalDateTime.now());
    }

    public List<NotificationCountResult> countNotifications(
            String productId, String from, String to, ExecutionContext context) {
        context
                .getLogger()
                .info(
                        () ->
                                String.format(
                                        "Starting countOnboarding with filters productId: %s from: %s to: %s",
                                        productId, from, to));
        return productService.getProducts(false, false).stream()
                .filter(product -> Objects.isNull(productId) || product.getId().equals(productId))
                .map(product -> countNotificationsByFilters(product.getId(), from, to, context))
                .toList();
    }

    public NotificationCountResult countNotificationsByFilters(
            String productId, String from, String to, ExecutionContext context) {
        Document queryAddEvent = getQueryNotificationAdd(productId, from, to);
        Document queryUpdateEvent = getQueryNotificationDelete(productId, from, to);

        long countAddEvents = onboardingRepositoryService.countByQuery(queryAddEvent);
        long countUpdateEvents = onboardingRepositoryService.countByQuery(queryUpdateEvent);
        long total = countUpdateEvents + countAddEvents;

        context
                .getLogger()
                .info(
                        () ->
                                String.format(
                                        "Counted onboardings for productId: %s add events: %s update events: %s",
                                        productId, countAddEvents, countUpdateEvents));
        return new NotificationCountResult(productId, total);
    }

    private Document getQueryNotificationDelete(String productId, String from, String to) {
        return createQuery(productId, List.of(OnboardingStatus.DELETED), from, to, DELETED_AT_FIELD);
    }

    private Document getQueryNotificationAdd(String productId, String from, String to) {
        return createQuery(
                productId,
                List.of(OnboardingStatus.COMPLETED, OnboardingStatus.DELETED),
                from,
                to,
                ACTIVATED_AT_FIELD);
    }

    private Document createQuery(
            String productId,
            List<OnboardingStatus> status,
            String from,
            String to,
            String dateField,
            boolean workflowTypeExist) {
        Document query = new Document();
        query.append("productId", productId);
        query.append(
                "status", new Document("$in", status.stream().map(OnboardingStatus::name).toList()));
        if (workflowTypeExist) {
            query.append(
                    WORKFLOW_TYPE,
                    new Document(
                            "$nin",
                            NOT_ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS.stream()
                                    .map(Enum::name)
                                    .toList()));
        } else {
            query.append(WORKFLOW_TYPE, new Document("$exists", false));
        }
        Document dateQuery = new Document();
        Optional.ofNullable(from)
                .ifPresent(
                        value ->
                                query.append(
                                        dateField,
                                        dateQuery.append(
                                                "$gte", LocalDate.parse(from, DateTimeFormatter.ISO_LOCAL_DATE))));
        Optional.ofNullable(to)
                .ifPresent(
                        value ->
                                query.append(
                                        dateField,
                                        dateQuery.append(
                                                "$lte",
                                                LocalDate.parse(to, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1))));
        if (!dateQuery.isEmpty()) {
            query.append(dateField, dateQuery);
        }
        return query;
    }

    private Document createQuery(
            String productId, List<OnboardingStatus> status, String from, String to, String dateField) {
        Document query = new Document();
        List<Document> workflowCriteria = new ArrayList<>();
        workflowCriteria.add(createQuery(productId, status, from, to, dateField, true));
        workflowCriteria.add(createQuery(productId, status, from, to, dateField, false));
        query.append("$or", workflowCriteria);
        return query;
    }

    public List<Onboarding> getOnboardingsToResend(
            ResendNotificationsFilters filters, int page, int pageSize) {
        return onboardingRepositoryService.findByQueryPaged(createQueryByFilters(filters), page, pageSize);
    }

    private Document createQueryByFilters(ResendNotificationsFilters filters) {
        Document query = new Document();
        Optional.ofNullable(filters.getProductId())
                .ifPresent(value -> query.append("productId", value));
        Optional.ofNullable(filters.getInstitutionId())
                .ifPresent(value -> query.append("institution.id", value));
        Optional.ofNullable(filters.getOnboardingId()).ifPresent(value -> query.append("_id", value));
        Optional.ofNullable(filters.getTaxCode())
                .ifPresent(value -> query.append("institution.taxCode", value));
        query.append("status", new Document("$in", filters.getStatus()));

        List<Document> dateQueries = createDateQueries(filters);
        List<Document> workflowCriteria = createWorkflowCriteria();

        query.append(
                "$and", List.of(new Document("$or", dateQueries), new Document("$or", workflowCriteria)));

        return query;
    }

    private List<Document> createDateQueries(ResendNotificationsFilters filters) {
        return Stream.of(
                        createIntervalQueryForDate(filters, ACTIVATED_AT_FIELD),
                        createIntervalQueryForDate(filters, DELETED_AT_FIELD))
                .filter(doc -> !doc.isEmpty())
                .toList();
    }

    private Document createIntervalQueryForDate(
            ResendNotificationsFilters filters, String dateField) {
        Document dateQuery = new Document();
        Optional.ofNullable(filters.getFrom())
                .ifPresent(
                        value ->
                                dateQuery.append("$gte", LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)));
        Optional.ofNullable(filters.getTo())
                .ifPresent(
                        value ->
                                dateQuery.append(
                                        "$lte", LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1)));
        return new Document(dateField, dateQuery);
    }

    private List<Document> createWorkflowCriteria() {
        return List.of(
                new Document(
                        WORKFLOW_TYPE,
                        new Document(
                                "$nin",
                                NOT_ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS.stream()
                                        .map(Enum::name)
                                        .toList())),
                new Document(WORKFLOW_TYPE, new Document("$exists", false)));
    }

    private void setManagerData(Onboarding onboarding, SendMailInput sendMailInput) {
        final String managerId =
                onboarding.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER == user.getRole())
                        .map(User::getId)
                        .findAny()
                        .orElse(null);

        List<UserInstitutionResponse> userInstitutions = getUserInstitutions(onboarding);
        if (!userInstitutions.isEmpty() &&
                userInstitutions.stream().anyMatch(userInstitution ->
                        userInstitution.getUserId().equals(onboarding.getPreviousManagerId()))) {
            UserResource previousManager =
                    pdvUserRegistryService.getUserById(
                            USERS_WORKS_FIELD_LIST, onboarding.getPreviousManagerId());
            sendMailInput.setPreviousManagerName(previousManager.getName().getValue());
            sendMailInput.setPreviousManagerSurname(previousManager.getFamilyName().getValue());
            UserResource currentManager =
                    pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, managerId);
            sendMailInput.setManagerName(currentManager.getName().getValue());
            sendMailInput.setManagerSurname(currentManager.getFamilyName().getValue());
        } else {
            onboarding.setPreviousManagerId(null);
        }
    }

    private List<UserInstitutionResponse> getUserInstitutions(Onboarding onboarding) {

        // Retrieve all onboardings for data in input
        List<Onboarding> onboardings = onboardingRepositoryService.findByFilters(
                onboarding.getInstitution().getTaxCode(),
                onboarding.getInstitution().getSubunitCode(),
                onboarding.getInstitution().getOrigin().name(),
                onboarding.getInstitution().getOriginId(),
                onboarding.getProductId());

        if (onboardings.isEmpty()) {
            return Collections.emptyList();
        }

        final String institutionId = onboardings.get(0).getInstitution().getId();
        return userInstitutionRestService.getActiveManagersByInstitutionAndProduct(
                institutionId, onboarding.getProductId(), OnboardedProductResponse.StatusEnum.ACTIVE);
    }

    public List<String> findByInstitutionAndProduct(String institutionId, String productId) {
        var onboardings = onboardingRepositoryService.findByOnboardingUsers(institutionId, productId);
        if (onboardings.isEmpty()) {
            return List.of();
        }
        return onboardings.stream()
                .flatMap(onboarding -> onboarding.getUsers().stream())
                .map(User::getId)
                .collect(Collectors.toSet())
                .stream()
                .toList();
    }

}
