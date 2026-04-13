package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta-cdi")
public interface UserMapper {

    @Mapping(source = "userResource.id", target = "id")
    UserId toUserId(UserResource userResource);

}
