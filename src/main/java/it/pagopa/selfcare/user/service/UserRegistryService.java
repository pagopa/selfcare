package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

public interface UserRegistryService {
    Uni<Void> updateUserRegistryAndSendNotificationToQueue(MutableUserFieldsDto userDto, String userId, String institutionId);

}
