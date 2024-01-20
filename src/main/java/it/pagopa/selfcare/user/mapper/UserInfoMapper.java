package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta", uses = {UserInstitutionRoleMapper.class})
public interface UserInfoMapper {

    UserInfoResponse toResponse(UserInfo userInfo);


}
