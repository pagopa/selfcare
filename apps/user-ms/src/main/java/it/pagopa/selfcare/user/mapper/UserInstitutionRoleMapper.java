package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.UserInstitutionRoleResponse;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface UserInstitutionRoleMapper {

    UserInstitutionRoleResponse toResponse(UserInstitutionRole model);
}
