package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.dto.NotificationUserToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.document_json.model.DocumentResponse;

public interface NotificationUserBuilder {
    NotificationUserToSend buildUserNotificationToSend(Onboarding onboarding, DocumentResponse document, InstitutionResponse institution,
                                                       String createdAt, String updatedAt, String status,
                                                       String userId, String partyRole, String productRole);

    default boolean shouldSendUserNotification(Onboarding onboarding, InstitutionResponse institution) {
        return false;
    }
}
