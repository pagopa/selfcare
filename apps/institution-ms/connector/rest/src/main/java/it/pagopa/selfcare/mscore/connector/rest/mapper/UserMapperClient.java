package it.pagopa.selfcare.mscore.connector.rest.mapper;

import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import it.pagopa.selfcare.mscore.model.user.User;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UpdateDescriptionDto;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserId;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;




@Mapper(componentModel = "spring")
public interface UserMapperClient {

    User toUser (UserResource userResource);

    @Mapping(target = "id", source = "userId.id")
    User fromUserId (UserId userId);

    @Mapping(target = "institutionDescription", source = "description")
    @Mapping(target = "institutionRootName", source = "parentDescription")
    UpdateDescriptionDto toUpdateDescriptionDto (InstitutionUpdate institutionUpdate);

}
