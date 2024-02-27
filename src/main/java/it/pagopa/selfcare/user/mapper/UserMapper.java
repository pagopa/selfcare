package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.controller.response.product.InstitutionProducts;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserNotificationResponse toUserNotification(UserNotificationToSend user);

    @Mapping(source = "userResource.fiscalCode", target = "taxCode")
    @Mapping(source = "userResource.familyName", target = "surname", qualifiedByName = "fromCertifiableString")
    @Mapping(source = "userResource.name", target = "name", qualifiedByName = "fromCertifiableString")
    @Mapping(target = "email", expression = "java(retrieveMailFromWorkContacts(userResource.getWorkContacts(), userMailUuid))")
    UserResponse toUserResponse(UserResource userResource, String userMailUuid);

    @Named("fromCertifiableString")
    default String fromCertifiableString(CertifiableFieldResourceOfstring certifiableFieldResourceOfstring) {
        return Optional.ofNullable(certifiableFieldResourceOfstring).map(CertifiableFieldResourceOfstring::getValue).orElse(null);
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

    MutableUserFieldsDto toMutableUserFieldsDto(UserResource userResource);

    @Mapping(source = "user.birthDate", target = "birthDate", qualifiedByName = "toCertifiableLocalDate")
    @Mapping(source = "user.familyName", target = "familyName",  qualifiedByName = "toCertifiableString")
    @Mapping(source = "user.name", target = "name",  qualifiedByName = "toCertifiableString")
    @Mapping(source = "user.fiscalCode", target = "fiscalCode")
    @Mapping(source = "workContactResource", target = "workContacts")
    SaveUserDto toSaveUserDto(CreateUserDto.User user, Map<String, WorkContactResource> workContactResource);

    @Named("toCertifiableLocalDate")
    default CertifiableFieldResourceOfLocalDate toLocalTime(String time) {
        var certifiableFieldResourceOfLocalDate = new CertifiableFieldResourceOfLocalDate();
        certifiableFieldResourceOfLocalDate.setValue(LocalDate.parse(time));
        certifiableFieldResourceOfLocalDate.setCertification(CertifiableFieldResourceOfLocalDate.CertificationEnum.NONE);
        return certifiableFieldResourceOfLocalDate;
    }

    @Named("toCertifiableString")
    default CertifiableFieldResourceOfstring toCertString(String value) {
        var certifiableFieldResourceOfstring = new CertifiableFieldResourceOfstring();
        certifiableFieldResourceOfstring.setValue(value);
        certifiableFieldResourceOfstring.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
        return certifiableFieldResourceOfstring;
    }
}
