package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.config.NotificationConfig;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FdNotificationBuilder extends BaseNotificationBuilder
    implements NotificationUserBuilder {
  public FdNotificationBuilder(
      String alternativeEmail,
      NotificationConfig.Consumer consumer,
      InstitutionApi proxyRegistryInstitutionApi,
      GeographicTaxonomiesApi geographicTaxonomiesApi,
      org.openapi.quarkus.core_json.api.InstitutionApi coreInstitutionApi) {
    super(
        alternativeEmail,
        consumer,
        proxyRegistryInstitutionApi,
        geographicTaxonomiesApi,
        coreInstitutionApi);
  }

  @Override
  public NotificationToSend buildNotificationToSend(
      Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent) {
    NotificationToSend notificationToSend =
        super.buildNotificationToSend(onboarding, token, institution, queueEvent);
    notificationToSend.setId(UUID.randomUUID().toString());
    notificationToSend.setInstitutionId(institution.getId());
    notificationToSend.setType(NotificationType.getNotificationTypeFromQueueEvent(queueEvent));
    notificationToSend.getInstitution().setFileName(notificationToSend.getFileName());
    notificationToSend.getInstitution().setContentType(notificationToSend.getContentType());
    return notificationToSend;
  }

  @Override
  public InstitutionToNotify retrieveInstitution(
      InstitutionResponse institution, Onboarding onboarding) {
    InstitutionToNotify institutionToNotify = super.retrieveInstitution(institution, onboarding);

    // Field not allowed in FD schema
    institutionToNotify.setCategory(null);
    return institutionToNotify;
  }

  @Override
  public BillingToSend retrieveBilling(Onboarding onboarding) {
    if (Objects.isNull(onboarding.getBilling())) {
      return null;
    }

    BillingToSend billing = super.retrieveBilling(onboarding);
    billing.setPublicService(onboarding.getBilling().isPublicServices());
    return billing;
  }

  @Override
  public NotificationUserToSend buildUserNotificationToSend(
      Onboarding onboarding,
      Token token,
      InstitutionResponse institution,
      String createdAt,
      String updatedAt,
      String status,
      String userId,
      String partyRole,
      String productRole) {
    NotificationToSend notification =
        buildNotificationToSend(onboarding, token, institution, QueueEvent.UPDATE);
    NotificationUserToSend notificationUserToSend = new NotificationUserToSend();
    notificationUserToSend.setId(notification.getId());
    notificationUserToSend.setInstitutionId(notification.getInstitutionId());
    notificationUserToSend.setProduct(notification.getProduct());
    notificationUserToSend.setOnboardingTokenId(notification.getOnboardingTokenId());
    notificationUserToSend.setCreatedAt(createdAt.endsWith("Z") ? createdAt : createdAt + "Z");
    notificationUserToSend.setUpdatedAt(updatedAt.endsWith("Z") ? updatedAt : updatedAt + "Z");
    QueueUserEvent queueUserEvent =
        switch (status) {
          case "DELETE" -> QueueUserEvent.DELETE_USER;
          case "SUSPEND" -> QueueUserEvent.SUSPEND_USER;
          default -> QueueUserEvent.ACTIVE_USER;
        };
    notificationUserToSend.setType(
        NotificationUserType.getNotificationTypeFromQueueEvent(queueUserEvent));
    UserToNotify user = new UserToNotify();
    user.setUserId(userId);
    user.setRole(partyRole);
    user.setRoles(List.of(productRole));
    notificationUserToSend.setUser(user);
    return notificationUserToSend;
  }

  @Override
  public boolean shouldSendUserNotification(
      Onboarding onboarding, InstitutionResponse institution) {
    return true;
  }
}
