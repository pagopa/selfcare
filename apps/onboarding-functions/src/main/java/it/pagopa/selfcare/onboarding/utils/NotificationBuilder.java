package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.dto.BillingToSend;
import it.pagopa.selfcare.onboarding.dto.InstitutionToNotify;
import it.pagopa.selfcare.onboarding.dto.NotificationToSend;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.document_json.model.DocumentResponse;

public interface NotificationBuilder {
  NotificationToSend buildNotificationToSend(
          Onboarding onboarding, DocumentResponse document, InstitutionResponse institution, QueueEvent queueEvent);

  default boolean shouldSendNotification(Onboarding onboarding, InstitutionResponse institution) {
    return true;
  }

  InstitutionToNotify retrieveInstitution(InstitutionResponse institution, Onboarding onboarding);

  void setTokenData(NotificationToSend notificationToSend, DocumentResponse document);

  void retrieveAndSetGeographicData(InstitutionToNotify institution);

  BillingToSend retrieveBilling(Onboarding onboarding);
}
