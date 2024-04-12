package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(target = "institutionName", source = "userInstitution.institutionDescription")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "state", source = "state")
    UserInstitutionRole toUserInstitutionRole(UserInstitution userInstitution, PartyRole role, OnboardedProductState state);
}
