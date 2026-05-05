package it.pagopa.selfcare.external_api.mapper;


import it.pagopa.selfcare.external_api.model.token.TokenUser;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TokenUserMapper {

    @Mapping(source = "id", target = "userId")
    TokenUser toEntity(UserResponse userResponse);

}
