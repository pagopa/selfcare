package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.dto.BillingToSend;
import it.pagopa.selfcare.onboarding.dto.InstitutionToNotify;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

public interface NotificationBuilder {
  NotificationToSend buildNotificationToSend(
      Onboarding onboarding, Token token, InstitutionResponse institution, QueueEvent queueEvent);

  default boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution) {
    return true;
  }

  InstitutionToNotify retrieveInstitution(InstitutionResponse institution, Onboarding onboarding);

  void setTokenData(NotificationToSend notificationToSend, Token token);

  void retrieveAndSetGeographicData(InstitutionToNotify institution);

  BillingToSend retrieveBilling(Onboarding onboarding);
}
