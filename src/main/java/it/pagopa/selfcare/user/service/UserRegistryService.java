package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

import java.util.List;

public interface UserRegistryService {
    Uni<List<UserNotificationToSend>> updateUserRegistryAndSendNotificationToQueue(UpdateUserRequest updateUserRequest, String userId, String institutionId);

}
