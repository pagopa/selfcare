package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.controller.response.UserDetailResponse;
import it.pagopa.selfcare.user.controller.response.UserNotificationResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.controller.response.product.InstitutionProducts;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserNotificationResponse toUserNotification(UserNotificationToSend user);

    @Mapping(source = "userResource.fiscalCode", target = "taxCode")
    @Mapping(source = "userResource.familyName", target = "surname")
    @Mapping(target = "email", expression = "java(retrieveMailFromWorkContacts(userResource.getWorkContacts(), userMailUuid))")
    UserResponse toUserResponse(UserResource userResource, String userMailUuid);
    default String fromCertifiabletoString(CertifiableFieldResourceOfstring certifiableFieldResourceOfstring) {
        return certifiableFieldResourceOfstring.getValue();
    }

    UserDetailResponse toUserResponse(UserResource userResource);

    @Named("retrieveMailFromWorkContacts")
    default String retrieveMailFromWorkContacts(Map<String, WorkContactResource> map, String userMailUuid){
        if(map!=null && !map.isEmpty() && map.containsKey(userMailUuid)){
            return map.get(userMailUuid).getEmail().getValue();
        }
        return null;
    }
    @Mapping(target = "id", source = "userId")
    @Mapping(target = "bindings", expression = "java(userInstitutionToBindings(userInstitution))")
    default UserProductsResponse toUserProductsResponse(UserInfo userInfoResponse) {
        UserProductsResponse response = new UserProductsResponse();
        if(userInfoResponse != null && !userInfoResponse.getInstitutions().isEmpty()) {
            response.setId(userInfoResponse.getUserId());

            List<InstitutionProducts> institutionProducts = userInfoResponse.getInstitutions().stream().map(userInstitution -> {
                InstitutionProducts institutionProduct = new InstitutionProducts();
                institutionProduct.setInstitutionId(userInstitution.getInstitutionId());
                institutionProduct.setInstitutionName(userInstitution.getInstitutionName());
                institutionProduct.setInstitutionRootName(userInstitution.getInstitutionRootName());

                OnboardedProductResponse product = new OnboardedProductResponse();
                product.setRole(userInstitution.getRole());
                product.setStatus(userInstitution.getStatus());

                institutionProduct.setProducts(List.of(product));
                return institutionProduct;
            }).toList();
            response.setBindings(institutionProducts);
        }

        return response;
    }

}
