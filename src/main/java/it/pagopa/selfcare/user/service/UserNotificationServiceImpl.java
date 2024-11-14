package it.pagopa.selfcare.user.service;

import com.microsoft.applicationinsights.TelemetryClient;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.utils.ProductUtils;
import it.pagopa.selfcare.user.client.EventHubFdRestClient;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.conf.CloudTemplateLoader;
import it.pagopa.selfcare.user.entity.UserInstitution;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;
import software.amazon.awssdk.utils.CollectionUtils;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.user.UserUtils.mapPropsForTrackEvent;
import static it.pagopa.selfcare.user.constant.TemplateMailConstant.*;
import static it.pagopa.selfcare.user.model.TrackEventInput.toTrackEventInput;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_PRODUCT_FAILURE;
import static it.pagopa.selfcare.user.model.constants.EventsMetric.EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS;
import static it.pagopa.selfcare.user.model.constants.EventsName.EVENT_USER_MS_NAME;


@Slf4j
@ApplicationScoped
public class UserNotificationServiceImpl implements UserNotificationService {

    @Inject
    @RestClient
    EventHubRestClient eventHubRestClient;

    @Inject
    @RestClient
    EventHubFdRestClient eventHubFdRestClient;

    @ConfigProperty(name = "user-ms.retry.min-backoff")
    Integer retryMinBackOff;

    @ConfigProperty(name = "user-ms.retry.max-backoff")
    Integer retryMaxBackOff;

    @ConfigProperty(name = "user-ms.retry")
    Integer maxRetry;


    private final MailService mailService;
    private final Configuration freemarkerConfig;
    private final boolean eventHubUsersEnabled;
    private final TelemetryClient telemetryClient;


    public UserNotificationServiceImpl(Configuration freemarkerConfig, CloudTemplateLoader cloudTemplateLoader, MailService mailService,
                                       @ConfigProperty(name = "user-ms.eventhub.users.enabled") boolean eventHubUsersEnabled,
                                       TelemetryClient telemetryClient) {
        this.mailService = mailService;
        this.freemarkerConfig = freemarkerConfig;
        this.telemetryClient = telemetryClient;
        freemarkerConfig.setTemplateLoader(cloudTemplateLoader);
        this.eventHubUsersEnabled = eventHubUsersEnabled;
    }

    @Override
    public Uni<UserNotificationToSend> sendKafkaNotification(UserNotificationToSend userNotificationToSend) {
        return eventHubUsersEnabled
                ? eventHubRestClient.sendMessage(userNotificationToSend)
                    .onItem().invoke(trackTelemetryEvent(userNotificationToSend, EVENTS_USER_INSTITUTION_PRODUCT_SUCCESS))
                    .onFailure().invoke(throwable -> log.warn("error during send dataLake notification for id {}: {} ", userNotificationToSend.getId(), throwable.getMessage(), throwable))
                    .onFailure().invoke(trackTelemetryEvent(userNotificationToSend, EVENTS_USER_INSTITUTION_PRODUCT_FAILURE))
                    .replaceWith(userNotificationToSend)
                : Uni.createFrom().item(userNotificationToSend);
    }

    private Runnable trackTelemetryEvent(UserNotificationToSend userNotificationToSend, String metricsName) {
        return () -> telemetryClient.trackEvent(EVENT_USER_MS_NAME, mapPropsForTrackEvent(toTrackEventInput(userNotificationToSend)), Map.of(metricsName, 1D));
    }

    @Override
    public Uni<Void> sendEmailNotification(UserResource user, UserInstitution institution, Product product, OnboardedProductState status, String productRole, String loggedUserName, String loggedUserSurname) {
        log.info("sendMailNotification {}", status.name());
        return switch (status) {
            case ACTIVE ->
                    buildDataModelAndSendEmail(user, institution, product, ACTIVATE_TEMPLATE, ACTIVATE_SUBJECT, productRole, loggedUserName, loggedUserSurname);
            case DELETED ->
                    buildDataModelAndSendEmail(user, institution, product, DELETE_TEMPLATE, DELETE_SUBJECT, productRole, loggedUserName, loggedUserSurname);
            case SUSPENDED ->
                    buildDataModelAndSendEmail(user, institution, product, SUSPEND_TEMPLATE, SUSPEND_SUBJECT, productRole, loggedUserName, loggedUserSurname);
            case PENDING, TOBEVALIDATED, REJECTED -> Uni.createFrom().voidItem();
        };
    }

    @Override
    public Uni<Void> sendCreateUserNotification(String institutionDescription, List<String> roleLabels, UserResource userResource, UserInstitution userInstitution, Product product, LoggedUser loggedUser) {
        log.debug("sendCreateNotification start");
        log.debug("sendCreateNotification institution = {}, productTitle = {}, email = {}", institutionDescription, product.getTitle(), userInstitution.getUserMailUuid());

        String email = retrieveMail(userResource, userInstitution);
        String templateName = roleLabels.size() > 1 ? CREATE_TEMPLATE_MULTIPLE_ROLE : CREATE_TEMPLATE_SINGLE_ROLE;
        Map<String, String> dataModel = buildCreateEmailDataModel(loggedUser, product, institutionDescription, roleLabels);

        return this.sendEmailNotification(templateName, CREATE_SUBJECT, email, dataModel)
                .onItem().invoke(() -> log.debug("sendCreateNotification end"));
    }

    private Map<String, String> buildCreateEmailDataModel(LoggedUser loggedUser, Product product, String institutionDescription, List<String> productRoleCodes) {
        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("requesterName", Optional.ofNullable(loggedUser.getName()).orElse(""));
        dataModel.put("requesterSurname", Optional.ofNullable(loggedUser.getFamilyName()).orElse(""));

        dataModel.put("productName", Optional.ofNullable(product.getTitle()).orElse(""));
        dataModel.put("institutionName", Optional.ofNullable(institutionDescription).orElse(""));
        if (productRoleCodes.size() > 1) {
            List<String> roleLabel = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(product.getAllRoleMappings())) {
                roleLabel = product.getAllRoleMappings().values().stream()
                        .flatMap(List::stream)
                        .filter(productRoleInfo -> !CollectionUtils.isNullOrEmpty(productRoleInfo.getRoles()))
                        .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream()
                                .filter(productRole -> productRoleCodes.contains(productRole.getCode())))
                        .map(ProductRole::getLabel)
                        .distinct()
                        .toList();
            }

            if(CollectionUtils.isNullOrEmpty(roleLabel)) {
                roleLabel = List.of("no_role_found");
            }

            dataModel.put("productRoles", roleLabel.stream().limit(productRoleCodes.size() - 1L)
                    .collect(Collectors.joining(", ")));
            if(roleLabel.size() > 1) {
                dataModel.put("lastProductRole", roleLabel.get(roleLabel.size() - 1));
            }

        } else {
            String roleLabel = "no_role_found";
            if (CollectionUtils.isNotEmpty(product.getAllRoleMappings())) {
                roleLabel = product.getAllRoleMappings().values().stream()
                        .flatMap(List::stream)
                        .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream())
                        .filter(productRole -> productRole.getCode().equals(productRoleCodes.get(0)))
                        .map(ProductRole::getLabel)
                        .findAny()
                        .orElse("no_role_found");
            }
            dataModel.put("productRole", roleLabel);
        }

        return dataModel;
    }

    private Map<String, String> buildEmailDataModel(UserInstitution institution, Product product, String givenProductRole, String loggedUserName, String loggedUserSurname) {
        Optional<OnboardedProduct> productDb = institution.getProducts().stream().filter(p -> StringUtils.equals(p.getProductId(), product.getId())
                && (StringUtils.isBlank(givenProductRole) || StringUtils.equals(p.getProductRole(), givenProductRole))).findFirst();

        Optional<String> roleLabel = Optional.empty();
        if (productDb.isPresent() ) {
            try {
                ProductRole productRole = ProductUtils.getProductRole(productDb.get().getProductRole(),
                        productDb.get().getRole(),
                        product);
                roleLabel = Optional.ofNullable(productRole.getLabel());
            } catch (IllegalArgumentException ignored) { }
        }

        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("productName", Optional.ofNullable(product.getTitle()).orElse(""));
        dataModel.put("productRole", roleLabel.orElse("no_role_found"));
        dataModel.put("institutionName", Optional.ofNullable(institution.getInstitutionDescription()).orElse(""));
        dataModel.put("requesterName", Optional.ofNullable(loggedUserName).orElse(""));
        dataModel.put("requesterSurname", Optional.ofNullable(loggedUserSurname).orElse(""));
        return dataModel;
    }

    private Uni<Void> buildDataModelAndSendEmail(org.openapi.quarkus.user_registry_json.model.UserResource user, UserInstitution institution, Product product, String templateName, String subject, String productRole, String loggedUserName, String loggedUserSurname) {
        String email = retrieveMail(user, institution);
        Map<String, String> dataModel = buildEmailDataModel(institution, product, productRole, loggedUserName, loggedUserSurname);
        return this.sendEmailNotification(templateName, subject, email, dataModel);
    }

    private Uni<Void> sendEmailNotification(String templateName, String subject, String email, Map<String, String> dataModel) {
        return Uni.createFrom().item(getContent(templateName, dataModel))
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transformToUni(content -> mailService.sendMail(email, content.toString(), subject));
    }

    private StringWriter getContent(String templateName, Map<String, String> dataModel) {
        StringWriter stringWriter = null;
        try {
            Template template = freemarkerConfig.getTemplate(templateName);
            stringWriter = new StringWriter();
            template.process(dataModel, stringWriter);
        } catch (Exception e) {
            log.error("Unable to fetch template {}", templateName, e);
        }
        return stringWriter;
    }

    private static String retrieveMail(UserResource user, UserInstitution institution) {
        WorkContactResource certEmail = Optional.ofNullable(user.getWorkContacts())
                .map(wc -> wc.getOrDefault(institution.getUserMailUuid(), null))
                .orElse(null);
        String email;
        if (certEmail == null || certEmail.getEmail() == null || StringUtils.isBlank(certEmail.getEmail().getValue())) {
            throw new InvalidRequestException("Missing mail for userId: " + user.getId());
        } else {
            email = certEmail.getEmail().getValue();
        }
        log.debug("retrieved Mail for user with id: {}", user.getId());
        return email;
    }
}