package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
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

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "createUserDto.institutionId", target = "institutionId")
    @Mapping(source = "createUserDto.institutionDescription", target = "institutionDescription")
    @Mapping(source = "createUserDto.institutionRootName", target = "institutionRootName")
    @Mapping(source = "userMailUuid", target = "userMailUuid")
    @Mapping(target = "products",  expression = "java(java.util.List.of(toNewOnboardedProduct(createUserDto.getProduct())))")
    UserInstitution toNewEntity(CreateUserDto createUserDto, String userId, String userMailUuid);

    @Mapping(target = "status",  expression = "java(it.pagopa.selfcare.user.constant.OnboardedProductState.ACTIVE)")
    @Mapping(target = "env",  expression = "java(it.pagopa.selfcare.onboarding.common.Env.ROOT)")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    OnboardedProduct toNewOnboardedProduct(CreateUserDto.Product product);
}
