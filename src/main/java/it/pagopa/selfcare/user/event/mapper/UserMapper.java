package it.pagopa.selfcare.user.event.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import it.pagopa.selfcare.user.event.entity.UserInfo;
import it.pagopa.selfcare.user.event.entity.UserInstitution;
import it.pagopa.selfcare.user.event.entity.UserInstitutionRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    @Mapping(target = "institutions", expression = "java(productToUserInstitutionRole(userInstitution, role, state))")
    UserInfo toNewUserInfo(UserInstitution userInstitution, PartyRole role, OnboardedProductState state);

    @Mapping(target = "institutionName", source = "userInstitution.institutionDescription")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "state", source = "state")
    UserInstitutionRole toUserInstitutionRole(UserInstitution userInstitution, PartyRole role, OnboardedProductState state);

    @Named("productToUserInstitutionRole")
    default List<UserInstitutionRole> productToUserInstitutionRole(UserInstitution userInstitution, PartyRole role, OnboardedProductState state) {
        UserInstitutionRole userInstitutionRole = new UserInstitutionRole();
        userInstitutionRole.setInstitutionId(userInstitution.getInstitutionId());
        userInstitutionRole.setInstitutionName(userInstitution.getInstitutionDescription());
        userInstitutionRole.setRole(role);
        userInstitutionRole.setState(state);
        return List.of(userInstitutionRole);
    }
}
