package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.UserInstitution;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "jakarta", uses = {OnboardedProductMapper.class})
public interface UserInstitutionMapper {

    @Mapping(target = "id", expression = "java(objectIdToString(userInstitution.getId()))")
    UserInstitutionResponse toResponse(UserInstitution userInstitution);


    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId.toHexString();
    }
}
