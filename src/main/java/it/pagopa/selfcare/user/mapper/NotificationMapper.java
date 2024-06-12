package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.UserToNotify;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface NotificationMapper {

    @Mapping(source = "onboardedProduct.tokenId", target = "onboardingTokenId")
    @Mapping(source = "onboardedProduct.productId", target = "productId")
    @Mapping(target = "updatedAt", expression = "java((null == onboardedProduct.getUpdatedAt()) ? onboardedProduct.getCreatedAt() : onboardedProduct.getUpdatedAt())")
    UserNotificationToSend setNotificationDetailsFromOnboardedProduct(UserToNotify user, OnboardedProduct onboardedProduct, String institutionId);



    @Mapping(source = "userResource.id", target = "userId")
    @Mapping(source = "userResource.name.value", target = "name")
    @Mapping(source = "userResource.familyName.value", target = "familyName")
    @Mapping(source = "userResource.email.value", target = "email")
    @Mapping(source = "onboardedProduct.role", target = "role")
    @Mapping(source = "onboardedProduct.productRole", target = "productRole")
    @Mapping(source = "onboardedProduct.status", target = "relationshipStatus")
    UserToNotify toUserNotify(UserResource userResource, OnboardedProduct onboardedProduct, String userId);
}
