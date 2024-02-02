package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

public interface UserEventService {
    Uni<Void> sendUpdateUserNotificationToQueue(MutableUserFieldsDto userDto, String userId, String institutionId);

}
