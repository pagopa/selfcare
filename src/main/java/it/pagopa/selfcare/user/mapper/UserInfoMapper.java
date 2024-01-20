package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "jakarta", uses = {UserInstitutionRoleMapper.class})
public interface UserInfoMapper {

    UserInfoResponse toResponse(UserInfo userInfo);

}
