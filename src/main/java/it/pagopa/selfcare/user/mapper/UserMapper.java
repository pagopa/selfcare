package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.response.OnboardedProductResponse;
import it.pagopa.selfcare.user.controller.response.UserNotificationResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.controller.response.product.InstitutionProducts;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
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
    @Mapping(target = "email", expression = "java(retrieveMailFromWorkContacts(userResource.getWorkContacts(), institutionId))")
    UserResponse toUserResponse(UserResource userResource, String institutionId);
    default String fromCertifiabletoString(CertifiableFieldResourceOfstring certifiableFieldResourceOfstring) {
        return certifiableFieldResourceOfstring.getValue();
    }

    @Named("retrieveMailFromWorkContacts")
    default String retrieveMailFromWorkContacts(Map<String, WorkContactResource> map, String institutionId){
        if(map!=null && !map.isEmpty() && map.containsKey(institutionId)){
            return map.get(institutionId).getEmail().getValue();
        }
        return null;
    }
    @Mapping(target = "id", source = "userId")
    @Mapping(target = "bindings", expression = "java(userInstitutionToBindings(userInstitution))")
    default UserProductsResponse toUserProductsResponse(List<UserInstitution> userInstitutions) {
        UserProductsResponse response = new UserProductsResponse();
        if(userInstitutions != null && !userInstitutions.isEmpty()) {
            response.setId(userInstitutions.get(0).getUserId());

            List<InstitutionProducts> institutionProducts = userInstitutions.stream().map(userInstitution -> {
                InstitutionProducts institutionProduct = new InstitutionProducts();
                institutionProduct.setInstitutionId(userInstitution.getInstitutionId());
                institutionProduct.setInstitutionName(userInstitution.getInstitutionDescription());
                institutionProduct.setProducts(toProducts(userInstitution.getProducts()));
                return institutionProduct;
            }).toList();
            response.setBindings(institutionProducts);
        }

        return response;
    }

    List<OnboardedProductResponse> toProducts(List<OnboardedProduct> products);

}
