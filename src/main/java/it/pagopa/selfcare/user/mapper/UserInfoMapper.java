package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "jakarta", uses = {UserInstitutionRoleMapper.class})
public interface UserInfoMapper {

    @Mapping(target = "id", expression = "java(objectIdToString(userInfo.getId()))")
    UserInfoResponse toResponse(UserInfo userInfo);

    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId.toHexString();
    }


}
