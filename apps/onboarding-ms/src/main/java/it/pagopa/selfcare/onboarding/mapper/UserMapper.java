package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(source = "name.value", target = "name")
    @Mapping(source = "familyName.value", target = "surname")
    @Mapping(source = "email.value", target = "email")
    @Mapping(source = "fiscalCode", target = "taxCode")
    void fillUserResponse(UserResource userResource, @MappingTarget UserResponse userResponse);

    UserResponse toUserResponse(User user);

}
