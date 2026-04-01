package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "userResource.id", target = "id")
    UserId toUserId(UserResource userResource);

}
