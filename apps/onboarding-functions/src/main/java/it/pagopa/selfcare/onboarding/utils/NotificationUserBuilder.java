package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.dto.NotificationUserToSend;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.InstitutionResponse;

import java.time.OffsetDateTime;

public interface NotificationUserBuilder {
    NotificationUserToSend buildUserNotificationToSend(Onboarding onboarding, Token token, InstitutionResponse institution,
                                                       String createdAt, String updatedAt, String status,
                                                       String userId, String partyRole, String productRole);

    default boolean shouldSendUserNotification(Onboarding onboarding, InstitutionResponse institution) {
        return false;
    }
}
