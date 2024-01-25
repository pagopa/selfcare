package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;

public interface UserEventService {
    Uni<Void> sendUpdateUserNotificationToQueue(String userId, String institutionId);

}
