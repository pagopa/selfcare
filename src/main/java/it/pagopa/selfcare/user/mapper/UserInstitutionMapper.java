package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    @Mapping(target = "products", expression = "java(toNewOnboardedProduct(createUserDto.getProduct()))")
    UserInstitution toNewEntity(CreateUserDto createUserDto, String userId, String userMailUuid);


    default List<OnboardedProduct> toNewOnboardedProduct(CreateUserDto.Product product) {
        if (product.getProductRoles().isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime now = java.time.LocalDateTime.now();
        return product.getProductRoles().stream()
                .map(role -> {
                    OnboardedProduct onboardedProduct = new OnboardedProduct();

                    onboardedProduct.setProductId(product.getProductId());
                    onboardedProduct.setTokenId(product.getTokenId());
                    onboardedProduct.setProductRole(role);
                    onboardedProduct.setRole(product.getRole());

                    onboardedProduct.setStatus(it.pagopa.selfcare.user.constant.OnboardedProductState.ACTIVE);
                    onboardedProduct.setEnv(it.pagopa.selfcare.onboarding.common.Env.ROOT);
                    onboardedProduct.setCreatedAt(now);
                    onboardedProduct.setUpdatedAt(now);

                    return onboardedProduct;
                })
                .collect(Collectors.toList());
    }
}
