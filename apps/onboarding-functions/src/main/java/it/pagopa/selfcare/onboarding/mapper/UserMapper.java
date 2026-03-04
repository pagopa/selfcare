package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.user_json.model.AddUserRoleDto;
import org.openapi.quarkus.user_json.model.PartyRole;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(source = "institution.id", target = "institutionId")
    @Mapping(source = "institution.description", target = "institutionDescription")
    @Mapping(source = "institution.parentDescription", target = "institutionRootName")
    @Mapping(target = "product", source = ".")
    AddUserRoleDto toUserRole(Onboarding onboarding);

    PartyRole toUserPartyRole(it.pagopa.selfcare.onboarding.common.PartyRole partyRole);


}
