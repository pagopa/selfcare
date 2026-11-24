package it.pagopa.selfcare.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import io.smallrye.context.api.ManagedExecutorConfig;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.utils.ProductUtils;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.conf.TenantNotificationConfig;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;
import org.openapi.quarkus.webhook_ms_json.api.WebhookApi;
import org.openapi.quarkus.webhook_ms_json.model.NotificationRequest;
import software.amazon.awssdk.utils.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.model.TrackEventInput.toTrackEventInput;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_PRODUCT_FAILURE;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_MS_NAME;

@Slf4j
@ApplicationScoped
public class UserNotificationServiceImpl implements UserNotificationService {

  public static final String REQUESTER_NAME = "requesterName";
  public static final String REQUESTER_SURNAME = "requesterSurname";
  public static final String PRODUCT_NAME = "productName";
  public static final String INSTITUTION_NAME = "institutionName";
  public static final String NO_ROLE_FOUND = "no_role_found";
  public static final String PRODUCT_ROLE = "productRole";
  public static final String DEFAULT_NAME = "Operatore PagoPa";
  public static final String SELFCARE_DASHBOARD_INSTITUTION_URL = "dashboardInstitutionUrl";

  @Inject @RestClient EventHubRestClient eventHubRestClient;

  @Inject @RestClient WebhookApi webhookApi;


  @Inject ObjectMapper objectMapper;

  @Inject TenantNotificationConfig tenantConfig;
  @Inject TenantContext tenantContext;

  @Inject
  @ManagedExecutorConfig(propagated = {}, cleared = ThreadContext.ALL_REMAINING)
  ManagedExecutor mailExecutor;

  @ConfigProperty(name = "user-ms.webhook.topic")
  String webhookTopic;

  private final TenantMailJob mailJob;
  private final TelemetryClient telemetryClient;

  public UserNotificationServiceImpl(
      TenantMailJob mailJob,
      TelemetryClient telemetryClient) {
    this.mailJob = mailJob;
    this.telemetryClient = telemetryClient;
  }

  @Override
  public Uni<UserNotificationToSend> sendKafkaNotification(
      UserNotificationToSend userNotificationToSend) {
    return tenantConfig.current().usersEnabled()
        ? eventHubRestClient
            .sendMessage(userNotificationToSend)
            .onItem()
            .invoke(
                trackTelemetryEvent(
                    userNotificationToSend, EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS))
            .onFailure()
            .invoke(
                throwable ->
                    log.warn(
                        "error during send dataLake notification for id {}: {} ",
                        userNotificationToSend.getId(),
                        throwable.getMessage(),
                        throwable))
            .onFailure()
            .invoke(
                trackTelemetryEvent(
                    userNotificationToSend, EVENTS_USER_INSTITUTION_PRODUCT_FAILURE))
            .replaceWith(userNotificationToSend)
        : Uni.createFrom().item(userNotificationToSend);
  }

  @Override
  public Uni<UserNotificationToSend> sendUserNotification(
      UserNotificationToSend userNotificationToSend) {
    return Uni.combine()
        .all()
        .unis(
            sendKafkaNotification(userNotificationToSend),
            sendWebhookNotification(userNotificationToSend))
        .discardItems()
        .replaceWith(userNotificationToSend);
  }

  Uni<UserNotificationToSend> sendWebhookNotification(
      UserNotificationToSend userNotificationToSend) {
    if (!tenantConfig.current().webhookEnabled()) {
      return Uni.createFrom().item(userNotificationToSend);
    }

    NotificationRequest notificationRequest;
    try {
      notificationRequest =
          NotificationRequest.builder()
              .productId(userNotificationToSend.getProductId())
              .tenantId(tenantContext.requiredTenantId())
              .payload(objectMapper.writeValueAsString(userNotificationToSend))
              .topic(webhookTopic)
              .build();
    } catch (JsonProcessingException e) {
      log.warn(
          "error during serialize webhook notification for id {}: {} ",
          userNotificationToSend.getId(),
          e.getMessage(),
          e);
      return Uni.createFrom().item(userNotificationToSend);
    }

    return webhookApi
        .sendNotification(notificationRequest)
        .onFailure()
        .invoke(
            throwable ->
                log.warn(
                    "error during send webhook notification for id {}: {} ",
                    userNotificationToSend.getId(),
                    throwable.getMessage(),
                    throwable))
        .onFailure()
        .recoverWithNull()
        .replaceWith(userNotificationToSend);
  }

  private Runnable trackTelemetryEvent(
      UserNotificationToSend userNotificationToSend, String metricsName) {
    return () ->
        telemetryClient.trackEvent(
            EVENT_USER_MS_NAME,
            mapPropsForTrackEvent(toTrackEventInput(userNotificationToSend)),
            Map.of(metricsName, 1D));
  }

  @Override
  public Uni<Void> sendEmailNotification(
      UserResource user,
      UserInstitution institution,
      Product product,
      OnboardedProductState status,
      String productRole,
      String loggedUserName,
      String loggedUserSurname) {
    log.info("sendMailNotification {}", status.name());
    return switch (status) {
      case ACTIVE ->
          buildDataModelAndSendEmail(
              user,
              institution,
              product,
              tenantConfig.current().templates().get("activated"),
              productRole,
              loggedUserName,
              loggedUserSurname);
      case DELETED ->
          buildDataModelAndSendEmail(
              user,
              institution,
              product,
              tenantConfig.current().templates().get("deleted"),
              productRole,
              loggedUserName,
              loggedUserSurname);
      case SUSPENDED ->
          buildDataModelAndSendEmail(
              user,
              institution,
              product,
              tenantConfig.current().templates().get("suspended"),
              productRole,
              loggedUserName,
              loggedUserSurname);
      case PENDING, TOBEVALIDATED, REJECTED -> Uni.createFrom().voidItem();
    };
  }

  @Override
  public Uni<Void> sendCreateUserNotification(
      String institutionDescription,
      List<String> roleLabels,
      UserResource userResource,
      UserInstitution userInstitution,
      Product product,
      LoggedUser loggedUser) {
    log.debug("sendCreateNotification start");
    log.debug(
        "sendCreateNotification institution = {}, productTitle = {}, email = {}",
        institutionDescription,
        product.getTitle(),
        userInstitution.getUserMailUuid());

    String email = retrieveMail(userResource, userInstitution);
    String templateId =
        tenantConfig.current().templates().get(roleLabels.size() > 1 ? "multi-role" : "single-role");
    Map<String, String> dataModel =
        buildCreateEmailDataModel(loggedUser, product, institutionDescription, roleLabels);

    return sendMail(userInstitution.getUserId(), email, templateId, dataModel);
  }

  private Map<String, String> buildCreateEmailDataModel(
      LoggedUser loggedUser,
      Product product,
      String institutionDescription,
      List<String> productRoleCodes) {
    Map<String, String> dataModel = new HashMap<>();

    if (Objects.equals(loggedUser.getName(), "apim")) {
      dataModel.put(REQUESTER_NAME, DEFAULT_NAME);
    } else {
      dataModel.put(REQUESTER_NAME, Optional.ofNullable(loggedUser.getName()).orElse(""));
    }

    dataModel.put(REQUESTER_SURNAME, Optional.ofNullable(loggedUser.getFamilyName()).orElse(""));

    dataModel.put(PRODUCT_NAME, Optional.ofNullable(product.getTitle()).orElse(""));
    dataModel.put(INSTITUTION_NAME, Optional.ofNullable(institutionDescription).orElse(""));
    if (productRoleCodes.size() > 1) {
      List<String> roleLabel = new ArrayList<>();

      if (CollectionUtils.isNotEmpty(product.getAllRoleMappings())) {
        roleLabel =
            product.getAllRoleMappings().values().stream()
                .flatMap(List::stream)
                .filter(
                    productRoleInfo -> !CollectionUtils.isNullOrEmpty(productRoleInfo.getRoles()))
                .flatMap(
                    productRoleInfo ->
                        productRoleInfo.getRoles().stream()
                            .filter(
                                productRole -> productRoleCodes.contains(productRole.getCode())))
                .map(ProductRole::getLabel)
                .distinct()
                .toList();
      }

      if (CollectionUtils.isNullOrEmpty(roleLabel)) {
        roleLabel = List.of(NO_ROLE_FOUND);
      }

      dataModel.put(
          "productRoles",
          roleLabel.stream().limit(productRoleCodes.size() - 1L).collect(Collectors.joining(", ")));
      if (roleLabel.size() > 1) {
        dataModel.put("lastProductRole", roleLabel.get(roleLabel.size() - 1));
      }

    } else {
      String roleLabel = NO_ROLE_FOUND;
      if (CollectionUtils.isNotEmpty(product.getAllRoleMappings())) {
        roleLabel =
            product.getAllRoleMappings().values().stream()
                .flatMap(List::stream)
                .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream())
                .filter(productRole -> productRole.getCode().equals(productRoleCodes.get(0)))
                .map(ProductRole::getLabel)
                .findAny()
                .orElse(NO_ROLE_FOUND);
      }
      dataModel.put(PRODUCT_ROLE, roleLabel);
    }

    return dataModel;
  }

  private Map<String, String> buildEmailDataModel(
      UserInstitution institution,
      Product product,
      String givenProductRole,
      String loggedUserName,
      String loggedUserSurname) {
    Optional<OnboardedProduct> productDb =
        institution.getProducts().stream()
            .filter(
                p ->
                    StringUtils.equals(p.getProductId(), product.getId())
                        && (StringUtils.isBlank(givenProductRole)
                            || StringUtils.equals(p.getProductRole(), givenProductRole)))
            .findFirst();

    Optional<String> roleLabel = Optional.empty();
    if (productDb.isPresent()) {
      try {
        ProductRole productRole =
            ProductUtils.getProductRole(
                productDb.get().getProductRole(), productDb.get().getRole(), product);
        roleLabel = Optional.ofNullable(productRole.getLabel());
      } catch (IllegalArgumentException ignored) {
      }
    }

    Map<String, String> dataModel = new HashMap<>();
    dataModel.put(PRODUCT_NAME, Optional.ofNullable(product.getTitle()).orElse(""));
    dataModel.put(PRODUCT_ROLE, roleLabel.orElse(NO_ROLE_FOUND));
    dataModel.put(
        INSTITUTION_NAME, Optional.ofNullable(institution.getInstitutionDescription()).orElse(""));
    dataModel.put(REQUESTER_NAME, Optional.ofNullable(loggedUserName).orElse(""));
    dataModel.put(REQUESTER_SURNAME, Optional.ofNullable(loggedUserSurname).orElse(""));
    return dataModel;
  }

  private Map<String, String> buildEmailDataModelUserRequest(
      UserInstitution institution, Product product) {
    String selfcareUrl = tenantConfig.current().selfcareUrl();
    Map<String, String> dataModel = new HashMap<>();
    dataModel.put(PRODUCT_NAME, Optional.ofNullable(product.getTitle()).orElse(""));
    dataModel.put(
        INSTITUTION_NAME, Optional.ofNullable(institution.getInstitutionDescription()).orElse(""));
    dataModel.put(
        SELFCARE_DASHBOARD_INSTITUTION_URL,
        Optional.ofNullable(institution.getInstitutionId())
            .map(id -> String.format("%s/dashboard/%s", selfcareUrl, id))
            .orElse(String.format("%s/dashboard/party-selection", selfcareUrl)));
    return dataModel;
  }

  private Uni<Void> buildDataModelAndSendEmail(
      org.openapi.quarkus.user_registry_json.model.UserResource user,
      UserInstitution institution,
      Product product,
      String templateId,
      String productRole,
      String loggedUserName,
      String loggedUserSurname) {
    String email = retrieveMail(user, institution);
    Map<String, String> dataModel =
        buildEmailDataModel(institution, product, productRole, loggedUserName, loggedUserSurname);
    return sendMail(user.getId().toString(), email, templateId, dataModel);
  }

  @Override
  public Uni<Void> buildDataModelRequestAndSendEmail(
      UserResource user, UserInstitution institution, Product product) {
    String email = retrieveMail(user, institution);
    Map<String, String> dataModel = buildEmailDataModelUserRequest(institution, product);
    return sendMail(user.getId().toString(), email, tenantConfig.current().templates().get("request"), dataModel);
  }

  @Override
  public Uni<Void> buildDataModelConventionRequestAndSendEmail(
      UserResource user, UserInstitution institution, Product product) {
    String email = retrieveMail(user, institution);
    Map<String, String> dataModel = buildEmailDataModelUserRequest(institution, product);
    return sendMail(user.getId().toString(), email, tenantConfig.current().templates().get("convention"), dataModel);
  }

  private Uni<Void> sendMail(String userId, String email, String templateId, Map<String, String> dataModel) {
    String tenant = tenantContext.requiredTenantId();
    Map<String, String> attributes = new HashMap<>(dataModel);
    return Uni.createFrom().voidItem().invoke(() ->
        mailExecutor.execute(() -> mailJob.send(tenant, userId, email, templateId, attributes)));
  }

  private static String retrieveMail(UserResource user, UserInstitution institution) {
    WorkContactResource certEmail =
        Optional.ofNullable(user.getWorkContacts())
            .map(wc -> wc.getOrDefault(institution.getUserMailUuid(), null))
            .orElse(null);
    String email;
    if (certEmail == null
        || certEmail.getEmail() == null
        || StringUtils.isBlank(certEmail.getEmail().getValue())) {
      throw new InvalidRequestException("Missing mail for userId: " + user.getId());
    } else {
      email = certEmail.getEmail().getValue();
    }
    log.debug("retrieved Mail for user with id: {}", user.getId());
    return email;
  }
}
