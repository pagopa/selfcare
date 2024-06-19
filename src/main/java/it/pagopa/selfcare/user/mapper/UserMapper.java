package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserNotificationResponse toUserNotification(UserNotificationToSend user);

    @Mapping(source = "userResource.fiscalCode", target = "taxCode")
    @Mapping(source = "userResource.familyName", target = "surname", qualifiedByName = "fromCertifiableString")
    @Mapping(source = "userResource.name", target = "name", qualifiedByName = "fromCertifiableString")
    @Mapping(target = "email", expression = "java(retrieveMailFromWorkContacts(userResource.getWorkContacts(), userMailUuid))")
    @Mapping(target = "workContacts", expression = "java(toWorkContacts(userResource.getWorkContacts()))")
    UserResponse toUserResponse(UserResource userResource, String userMailUuid);

    @Mapping(target = "email", expression = "java(retrieveCertifiedMailFromWorkContacts(userResource, userMailUuid))")
    @Mapping(source = "userResource.familyName", target = "familyName", qualifiedByName = "toCertifiableFieldResponse")
    @Mapping(source = "userResource.name", target = "name", qualifiedByName = "toCertifiableFieldResponse")
    @Mapping(target = "workContacts", expression = "java(toWorkContactResponse(userResource.getWorkContacts()))")
    UserDetailResponse toUserDetailResponse(UserResource userResource, String userMailUuid);

    default Map<String, String> toWorkContacts(Map<String, WorkContactResource> workContactResourceMap) {
        if (workContactResourceMap == null){
            return Collections.emptyMap();
        }
        return workContactResourceMap.entrySet().stream()
                .filter(entry -> entry.getValue().getEmail() != null && entry.getValue().getEmail().getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getEmail().getValue()));
    }


    @Named("fromCertifiableString")
    default String fromCertifiableString(CertifiableFieldResourceOfstring certifiableFieldResourceOfstring) {
        return Optional.ofNullable(certifiableFieldResourceOfstring).map(CertifiableFieldResourceOfstring::getValue).orElse(null);
    }

    @Named("toWorkContactResponse")
    default Map<String, WorkContactResponse> toWorkContactResponse(Map<String, WorkContactResource> workContactResourceMap){
        Map<String, WorkContactResponse> resourceMap = new HashMap<>();
        if (workContactResourceMap != null && !workContactResourceMap.isEmpty()) {
            workContactResourceMap.forEach((key, value) -> {
                WorkContactResponse workContact = new WorkContactResponse();
                workContact.setEmail(toCertifiableFieldResponse(value.getEmail()));
                resourceMap.put(key, workContact);
            });
        }
        return resourceMap;
    }

    @Named("retrieveMailFromWorkContacts")
    default String retrieveMailFromWorkContacts(Map<String, WorkContactResource> map, String userMailUuid){
        if(map!=null && !map.isEmpty() && map.containsKey(userMailUuid)){
            return map.get(userMailUuid).getEmail().getValue();
        }
        return null;
    }


    @Named("retrieveCertifiedMailFromWorkContacts")
    default CertifiableFieldResponse<String> retrieveCertifiedMailFromWorkContacts(UserResource userResource, String userMailUuid){
        if(userResource.getWorkContacts()!=null && !userResource.getWorkContacts().isEmpty() && userResource.getWorkContacts().containsKey(userMailUuid)){
            return new CertifiableFieldResponse<>(userResource.getWorkContacts().get(userMailUuid).getEmail().getValue(), userResource.getWorkContacts().get(userMailUuid).getEmail().getCertification());
        }
        return null;
    }

    @Named("toCertifiableFieldResponse")
    default CertifiableFieldResponse<String> toCertifiableFieldResponse(CertifiableFieldResourceOfstring resource){
        return Optional.ofNullable(resource).map(r -> new CertifiableFieldResponse<>(r.getValue(), r.getCertification())).orElse(null);
    }
    MutableUserFieldsDto toMutableUserFieldsDto(UserResource userResource);

    @Mapping(target = "familyName",  expression = "java(toCertifiableStringNotEquals(userResource.getFamilyName(), updateUserRequest.getFamilyName()))")
    @Mapping(target = "name",  expression = "java(toCertifiableStringNotEquals(userResource.getName(), updateUserRequest.getName()))")
    @Mapping(target = "workContacts",  expression = "java(toWorkContact(updateUserRequest.getEmail(), idMail))")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    MutableUserFieldsDto toMutableUserFieldsDto(UpdateUserRequest updateUserRequest, UserResource userResource, String idMail);

    @Mapping(source = "user.birthDate", target = "birthDate", qualifiedByName = "toCertifiableLocalDate")
    @Mapping(source = "user.familyName", target = "familyName",  qualifiedByName = "toCertifiableString")
    @Mapping(source = "user.name", target = "name",  qualifiedByName = "toCertifiableString")
    @Mapping(source = "user.fiscalCode", target = "fiscalCode")
    @Mapping(source = "workContactResource", target = "workContacts")
    SaveUserDto toSaveUserDto(CreateUserDto.User user, Map<String, WorkContactResource> workContactResource);

    @Named("toWorkContact")
    default Map<String, WorkContactResource> toWorkContact(String email, String idMail){
        if (StringUtils.isNotBlank(idMail) && StringUtils.isNotBlank(email)){
            WorkContactResource workContactResource = new WorkContactResource();
            workContactResource.setEmail(toCertString(email));
            return Map.of(idMail, workContactResource);
        }
        return null;
    }
    @Named("toCertifiableLocalDate")
    default CertifiableFieldResourceOfLocalDate toLocalTime(String time) {
        if(Objects.isNull(time)) return null;
        var certifiableFieldResourceOfLocalDate = new CertifiableFieldResourceOfLocalDate();
        certifiableFieldResourceOfLocalDate.setValue(LocalDate.parse(time));
        certifiableFieldResourceOfLocalDate.setCertification(CertifiableFieldResourceOfLocalDate.CertificationEnum.NONE);
        return certifiableFieldResourceOfLocalDate;
    }

    @Named("toCertifiableString")
    default CertifiableFieldResourceOfstring toCertString(String value) {
        if (StringUtils.isNotBlank(value)){
            var certifiableFieldResourceOfstring = new CertifiableFieldResourceOfstring();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    @Named("toCertifiableStringNotEquals")
    default CertifiableFieldResourceOfstring toCertifiableStringNotEquals(CertifiableFieldResourceOfstring certifiableString, String value) {
        if(StringUtils.isBlank(value) ||
                Objects.nonNull(certifiableString) && CertifiableFieldResourceOfstring.CertificationEnum.SPID.equals(certifiableString.getCertification())){
            return null;
        }

        if(Objects.isNull(certifiableString) || !value.equals(certifiableString.getValue())){
            var certifiableFieldResourceOfstring = new CertifiableFieldResourceOfstring();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    OnboardedProductMapper productMapper = Mappers.getMapper(OnboardedProductMapper.class);
    @Mapping(target = "id", expression = "java(userInstitution.getId().toString())")
    @Mapping(target = "role", expression = "java(getMaxRole(userInstitution.getProducts()))")
    @Mapping(target = "status", expression = "java(getMaxStatus(userInstitution.getProducts()))")
    @Mapping(target = "products", expression = "java(productMapper.toList(userInstitution.getProducts()))")
    @Mapping(target = "userResponse", expression = "java(toUserResponse(userResource, userInstitution.getUserMailUuid()))")
    UserDataResponse toUserDataResponse(UserInstitution userInstitution, UserResource userResource);

    @Named("getMaxStatus")
    default String getMaxStatus(List<OnboardedProduct> onboardedProductList){
        List<OnboardedProductState> onboardedProductStateList = onboardedProductList.stream().map(OnboardedProduct::getStatus).toList();
        return Collections.min(onboardedProductStateList).name();
    }

    @Named("getMaxRole")
    default String getMaxRole(List<OnboardedProduct> onboardedProductList){
        List<PartyRole> partyRoleList = onboardedProductList.stream().map(OnboardedProduct::getRole).toList();
        return Collections.min(partyRoleList).name();
    }

    UserInfoResponse toUserInfoResponse(UserInfo userInfo);
}
