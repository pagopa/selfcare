package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.UserUtils;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.*;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.user_registry_json.model.EmailCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "cdi", imports = UUID.class)
public interface NotificationMapper {

    @Mapping(target = "onboardingTokenId", source = "product.tokenId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", expression = "java((null == product.getUpdatedAt()) ? product.getCreatedAt() : product.getUpdatedAt())")
    @Mapping(target = "user", expression = "java(mapUser(userResource, userInstitution.getUserMailUuid(), product))")
    @Mapping(target = "id", expression = "java(toUniqueIdNotification(userInstitution, product))")
    @Mapping(target = "eventType", expression = "java(it.pagopa.selfcare.user.model.constants.QueueEvent.UPDATE)")
    UserNotificationToSend toUserNotificationToSend(UserInstitution userInstitution, it.pagopa.selfcare.user.model.OnboardedProduct product, UserResource userResource);

    @Named("toUniqueIdNotification")
    default String toUniqueIdNotification(UserInstitution userInstitution, OnboardedProduct product) {
        return UserUtils.uniqueIdNotification(userInstitution.getId().toHexString(), product.getProductId(), product.getProductRole());
    }

    @Mapping(target = "id", expression = "java(toUniqueIdNotification(userInstitutionChanged, product))")
    @Mapping(target = "onboardingTokenId", source = "product.tokenId")
    @Mapping(target = "product", source = "product.productId")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", expression = "java((null == product.getUpdatedAt()) ? product.getCreatedAt() : product.getUpdatedAt())")
    @Mapping(target = "user", expression = "java(mapUserForFD(userId, product))")
    @Mapping(target = "type", source = "type")
    FdUserNotificationToSend toFdUserNotificationToSend(UserInstitution userInstitutionChanged, OnboardedProduct product, String userId, NotificationUserType type);

    @Named("mapUserForFD")
    default UserToNotify mapUserForFD(String userId, OnboardedProduct onboardedProduct) {
        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId);
        userToNotify.setRoles(StringUtils.isNotBlank(onboardedProduct.getProductRole()) ? List.of(onboardedProduct.getProductRole()) : Collections.emptyList());
        userToNotify.setRole(Optional.ofNullable(onboardedProduct.getRole()).map(Enum::name).orElse(null));
        return userToNotify;
    }

    @Named("mapUser")
    default UserToNotify mapUser(UserResource userResource, String userMailUuid, OnboardedProduct onboardedProduct) {
        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(Optional.ofNullable(userResource.getId()).map(UUID::toString).orElse(null));
        userToNotify.setName(Optional.ofNullable(userResource.getName()).map(NameCertifiableSchema::getValue).orElse(null));
        userToNotify.setFamilyName(Optional.ofNullable(userResource.getFamilyName()).map(FamilyNameCertifiableSchema::getValue).orElse(null));
        userToNotify.setEmail(Optional.ofNullable(userMailUuid).map(mailUuid -> retrieveMailFromWorkContacts(userResource, mailUuid)).orElse(null));
        userToNotify.setProductRole(onboardedProduct.getProductRole());
        userToNotify.setRole(Optional.ofNullable(onboardedProduct.getRole()).map(Enum::name).orElse(null));
        userToNotify.setRelationshipStatus(onboardedProduct.getStatus());
        return userToNotify;
    }

    default String retrieveMailFromWorkContacts(UserResource userResource, String userMailUuid) {
        return Optional.ofNullable(userResource.getWorkContacts())
                .flatMap(stringWorkContactResourceMap -> Optional.ofNullable(stringWorkContactResourceMap.get(userMailUuid))
                        .flatMap(workContactResource -> Optional.ofNullable(workContactResource.getEmail())
                                .map(EmailCertifiableSchema::getValue)))
                .orElse(null);
    }
}
