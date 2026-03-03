package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.TokenType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePathConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.GenericError;
import it.pagopa.selfcare.onboarding.utils.InstitutionUtils;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static it.pagopa.selfcare.onboarding.utils.Utils.CONTRACT_FILENAME_FUNC;
import static it.pagopa.selfcare.onboarding.utils.Utils.NOT_ALLOWED_WORKFLOWS_FOR_INSTITUTION_NOTIFICATIONS;

@ApplicationScoped
public class OnboardingService {

  private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);
  public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name";
  public static final String USERS_WORKS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
  public static final String USER_REQUEST_DOES_NOT_FOUND =
    "User request does not found for onboarding %s";
  public static final String ACTIVATED_AT_FIELD = "activatedAt";
  public static final String DELETED_AT_FIELD = "deletedAt";
  private static final String WORKFLOW_TYPE = "workflowType";

  @RestClient @Inject UserApi userRegistryApi;
  @RestClient @Inject InstitutionApi userInstitutionApi;
  @RestClient @Inject
  org.openapi.quarkus.user_json.api.UserApi userApi;
  @RestClient @Inject
  PdndVisuraInfoCamereControllerApi pndnInfocamereApi;
  @Inject NotificationService notificationService;
  private final ContractService contractService;
  private final ProductService productService;
  private final OnboardingRepository repository;
  private final TokenRepository tokenRepository;
  private final MailTemplatePathConfig mailTemplatePathConfig;
  private final MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;
  private final AzureBlobClient azureBlobClient;
  private final UserMapper userMapper;
  private final AzureStorageConfig azureStorageConfig;

  public OnboardingService(
          ProductService productService,
          ContractService contractService,
          OnboardingRepository repository,
          MailTemplatePathConfig mailTemplatePathConfig,
          MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig,
          TokenRepository tokenRepository,
          NotificationService notificationService,
          UserMapper userMapper,
          AzureBlobClient azureBlobClient,
          AzureStorageConfig azureStorageConfig) {
    this.contractService = contractService;
    this.repository = repository;
    this.tokenRepository = tokenRepository;
    this.productService = productService;
    this.notificationService = notificationService;
    this.mailTemplatePathConfig = mailTemplatePathConfig;
    this.mailTemplatePlaceholdersConfig = mailTemplatePlaceholdersConfig;
    this.userMapper = userMapper;
    this.azureBlobClient = azureBlobClient;
    this.azureStorageConfig = azureStorageConfig;
  }

  public Optional<Onboarding> getOnboarding(String onboardingId) {
    return repository.findByIdOptional(onboardingId);
  }

  public void createContract(OnboardingWorkflow onboardingWorkflow) {
    Onboarding onboarding = onboardingWorkflow.getOnboarding();

    List<UserResource> delegates =
      onboarding.getUsers().stream()
        .filter(userToOnboard -> PartyRole.MANAGER != userToOnboard.getRole())
        .map(
          userToOnboard ->
            userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, userToOnboard.getId()))
        .toList();

    Product product = productService.getProductIsValid(onboarding.getProductId());
    contractService.createContractPDF(
      onboardingWorkflow.getContractTemplatePath(product),
      onboarding,
      getUserResource(onboarding),
      delegates,
      product.getTitle(),
      onboardingWorkflow.getPdfFormatFilename());
  }

  private UserResource getUserResource(Onboarding onboarding) {
    String validManagerId = getValidManagerId(onboarding.getUsers());
    return userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, validManagerId);
  }

  public void createAttachment(OnboardingAttachment onboardingAttachment) {
    Onboarding onboarding = onboardingAttachment.getOnboarding();
    Product product = productService.getProductIsValid(onboarding.getProductId());
    AttachmentTemplate attachment = onboardingAttachment.getAttachment();

    contractService.createAttachmentPDF(
      attachment.getTemplatePath(), onboarding, product.getTitle(), attachment.getName(), getUserResource(onboarding));
  }

  public void loadContract(Onboarding onboarding) {
    Product product = productService.getProductIsValid(onboarding.getProductId());
    contractService.loadContractPDF(
      product
        .getInstitutionContractTemplate(InstitutionUtils.getCurrentInstitutionType(onboarding))
        .getContractTemplatePath(),
      onboarding.getId(),
      product.getTitle());
  }

  public void saveTokenWithContract(OnboardingWorkflow onboardingWorkflow) {

    Onboarding onboarding = onboardingWorkflow.getOnboarding();

    // Skip if token already exists
    if (checkTokenExist(onboarding)) {
      return;
    }

    Product product = productService.getProductIsValid(onboarding.getProductId());

    // Load PDF contract and create digest
    File contract =
      contractService.retrieveContractNotSigned(onboardingWorkflow, product.getTitle());
    DSSDocument document = new FileDocument(contract);
    String digest = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();

    saveToken(onboardingWorkflow, product, digest);
  }

  public void saveTokenWithAttachment(OnboardingAttachment onboardingAttachment) {

    Onboarding onboarding = onboardingAttachment.getOnboarding();

    Product product = productService.getProductIsValid(onboarding.getProductId());

    File contract = contractService.retrieveAttachment(onboardingAttachment, product.getTitle());
    DSSDocument document = new FileDocument(contract);
    String digest = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();

    saveTokenAttachment(onboardingAttachment, product, digest);
  }

  private boolean checkTokenExist(Onboarding onboarding) {
    // Skip if token already exists
    Optional<Token> optToken = tokenRepository.findByIdOptional(onboarding.getId());
    if (optToken.isPresent()) {
      log.debug("Token has already exists for onboarding {}", onboarding.getId());
      return true;
    }
    return false;
  }

  public Optional<Token> getToken(String onboardingId) {
    return tokenRepository.findByIdOptional(onboardingId);
  }

  private void saveToken(OnboardingWorkflow onboardingWorkflow, Product product, String digest) {

    Onboarding onboarding = onboardingWorkflow.getOnboarding();

    // Persist token entity
    Token token = buildBaseToken(onboarding, digest);
    token.setId(onboarding.getId());
    token.setContractTemplate(onboardingWorkflow.getContractTemplatePath(product));
    token.setContractVersion(onboardingWorkflow.getContractTemplateVersion(product));
    token.setContractFilename(
      CONTRACT_FILENAME_FUNC.apply(
        onboardingWorkflow.getPdfFormatFilename(), product.getTitle()));
    token.setType(onboardingWorkflow.getTokenType());

    tokenRepository.persist(token);
  }

  private void saveTokenAttachment(
    OnboardingAttachment onboardingAttachment, Product product, String digest) {

    Onboarding onboarding = onboardingAttachment.getOnboarding();
    AttachmentTemplate attachmentTemplate = onboardingAttachment.getAttachment();

    // Persist token entity
    Token token = buildBaseToken(onboarding, digest);
    token.setId(UUID.randomUUID().toString());
    token.setName(attachmentTemplate.getName());
    token.setContractTemplate(attachmentTemplate.getTemplatePath());
    token.setContractVersion(attachmentTemplate.getTemplateVersion());
    token.setContractFilename(
      CONTRACT_FILENAME_FUNC.apply(
        "%s_" + attachmentTemplate.getName() + ".pdf", product.getTitle()));
    token.setType(TokenType.ATTACHMENT);

    tokenRepository.persist(token);
  }

  private Token buildBaseToken(Onboarding onboarding, String digest) {
    log.debug("creating Token for onboarding {} ...", onboarding.getId());
    Token token = new Token();
    token.setOnboardingId(onboarding.getId());
    token.setCreatedAt(LocalDateTime.now());
    token.setUpdatedAt(LocalDateTime.now());
    token.setChecksum(digest);
    token.setProductId(onboarding.getProductId());
    return token;
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

   try {
     userApi.sendMailRequest(user.getId(), sendMailDto);
   } catch (Exception e) {
     log.error("Impossible to send mail to user");
   }
  }

    public void sendMailRegistrationForUserRequester(Onboarding onboarding) {

        log.info("Sending mail to user requester");
        UserRequester userRequester = onboarding.getUserRequester();
        SendMailDto sendMailDto = new SendMailDto();
        sendMailDto.setInstitutionName(onboarding.getInstitution().getDescription());
        sendMailDto.setProductId(onboarding.getProductId());
        sendMailDto.setUserMailUuid(userRequester.getUserMailUuid());

        try {
            userApi.sendMailRequest(userRequester.getUserRequestUid(), sendMailDto);
        } catch (Exception e) {
            log.error("Impossible to send mail to user");
        }
    }

  public void saveVisuraForMerchant(Onboarding onboarding) {
    var taxCode = onboarding.getInstitution().getTaxCode();
    try {
      var bytes = pndnInfocamereApi.institutionVisuraDocumentByTaxCodeUsingGET(taxCode);
      final String filename = String.format("VISURA_%s.xml", taxCode);
      final String path = String.format("%s%s%s", azureStorageConfig.contractPath(), onboarding.getId(), "/visura");
      azureBlobClient.uploadFile(path, filename, bytes);
    } catch (Exception e) {
      log.error("Impossible to store visura document for institution with taxCode: {}. Error: {}", taxCode, e.getMessage(), e);
    }
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
    repository.update(onboarding);
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
          userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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

  public long updateTokenContractFiles(Token token) {
    Map<String, Object> paramsUpdate = new HashMap<>();
    paramsUpdate.put("contractSigned", token.getContractSigned());
    paramsUpdate.put("contractFilename", token.getContractFilename());
    paramsUpdate.put("updatedAt", LocalDateTime.now());

    Map<String, Object> paramsWhere = new HashMap<>();
    paramsWhere.put("tokenId", token.getId());

    return tokenRepository
      .update ("contractSigned = :contractSigned and contractFilename = :contractFilename and updatedAt = :updatedAt", paramsUpdate)
      .where("_id = :tokenId", paramsWhere);
  }

  public void updateOnboardingStatus(String onboardingId, OnboardingStatus status) {
    repository
      .update("status = ?1 and updatedAt = ?2", status.name(), LocalDateTime.now())
      .where("_id", onboardingId);
  }

  public void updateOnboardingStatusAndInstanceId(
    String onboardingId, OnboardingStatus status, String instanceId) {
    repository
      .update(
        "status = ?1 and workflowInstanceId = ?2 and updatedAt = ?3",
        status.name(),
        instanceId,
        LocalDateTime.now())
      .where("_id", onboardingId);
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

    long countAddEvents = repository.find(queryAddEvent).count();
    long countUpdateEvents = repository.find(queryUpdateEvent).count();
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
    return repository.find(createQueryByFilters(filters)).page(page, pageSize).list();
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
        userRegistryApi.findByIdUsingGET(
          USERS_WORKS_FIELD_LIST, onboarding.getPreviousManagerId());
      sendMailInput.setPreviousManagerName(previousManager.getName().getValue());
      sendMailInput.setPreviousManagerSurname(previousManager.getFamilyName().getValue());
      UserResource currentManager =
        userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, managerId);
      sendMailInput.setManagerName(currentManager.getName().getValue());
      sendMailInput.setManagerSurname(currentManager.getFamilyName().getValue());
    } else {
      onboarding.setPreviousManagerId(null);
    }
  }

  private List<UserInstitutionResponse> getUserInstitutions(Onboarding onboarding) {

    // Retrieve all onboardings for data in input
    List<Onboarding> onboardings = repository.findByFilters(
      onboarding.getInstitution().getTaxCode(),
      onboarding.getInstitution().getSubunitCode(),
      onboarding.getInstitution().getOrigin().name(),
      onboarding.getInstitution().getOriginId(),
      onboarding.getProductId());

    if (onboardings.isEmpty()) {
      return Collections.emptyList();
    }

    final String institutionId = onboardings.get(0).getInstitution().getId();
    return userInstitutionApi.retrieveUserInstitutions(
      institutionId,
      null,
      List.of(onboarding.getProductId()),
      List.of(String.valueOf(PartyRole.MANAGER)),
      List.of(String.valueOf(OnboardedProductResponse.StatusEnum.ACTIVE)),
      null);
  }

}
