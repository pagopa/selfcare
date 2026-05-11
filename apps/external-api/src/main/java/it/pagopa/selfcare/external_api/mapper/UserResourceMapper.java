package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.user.UserToOnboard;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserResourceMapper {

    @Mapping(target = "fiscalCode", source = "taxCode")
    @Mapping(target = "familyName", source = "surname")
    @Mapping(target = "institutionEmail", source = "email")
    User toUser(UserToOnboard user);
}
