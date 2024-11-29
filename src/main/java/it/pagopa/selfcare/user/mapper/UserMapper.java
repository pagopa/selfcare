package it.pagopa.selfcare.user.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.CertificationEnum;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.openapi.quarkus.user_registry_json.model.BirthDateCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.EmailCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.MobilePhoneCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface UserMapper {

    UserNotificationResponse toUserNotification(UserNotificationToSend user);

    @Mapping(source = "userResource.fiscalCode", target = "taxCode")
    @Mapping(source = "userResource.familyName", target = "surname", qualifiedByName = "fromSurnameCertifiableString")
    @Mapping(source = "userResource.name", target = "name", qualifiedByName = "fromNameCertifiableString")
    @Mapping(target = "email", expression = "java(retrieveMailFromWorkContacts(userResource.getWorkContacts(), userMailUuid))")
    @Mapping(target = "mobilePhone", expression = "java(retrieveMobilePhoneFromWorkContacts(userResource.getWorkContacts(), userMailUuid))")
    @Mapping(target = "workContacts", expression = "java(toWorkContacts(userResource.getWorkContacts()))")
    UserResponse toUserResponse(UserResource userResource, String userMailUuid);

    @Mapping(target = "email", expression = "java(retrieveCertifiedMailFromWorkContacts(userResource, userMailUuid))")
    @Mapping(target = "mobilePhone", expression = "java(retrieveCertifiedMobilePhoneFromWorkContacts(userResource, userMailUuid))")
    @Mapping(source = "userResource.familyName", target = "familyName", qualifiedByName = "toFamilyNameCertifiableFieldResponse")
    @Mapping(source = "userResource.name", target = "name", qualifiedByName = "toNameCertifiableFieldResponse")
    @Mapping(target = "workContacts", expression = "java(toWorkContactResponse(userResource.getWorkContacts()))")
    UserDetailResponse toUserDetailResponse(UserResource userResource, String userMailUuid);

    default Map<String, String> toWorkContacts(Map<String, WorkContactResource> workContactResourceMap) {
        if (workContactResourceMap == null){
            return Collections.emptyMap();
        }
        return workContactResourceMap.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(entry -> Objects.nonNull(entry.getValue().getEmail()))
                .filter(entry -> Objects.nonNull(entry.getValue().getEmail().getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getEmail().getValue()));
    }


    @Named("fromSurnameCertifiableString")
    default String fromCertifiableString(org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema certifiableFieldResourceOfstring) {
        return Optional.ofNullable(certifiableFieldResourceOfstring).map(org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema::getValue).orElse(null);
    }

    @Named("fromNameCertifiableString")
    default String fromCertifiableString(NameCertifiableSchema certifiableFieldResourceOfstring) {
        return Optional.ofNullable(certifiableFieldResourceOfstring).map(NameCertifiableSchema::getValue).orElse(null);
    }

    @Named("toWorkContactResponse")
    default Map<String, WorkContactResponse> toWorkContactResponse(Map<String, WorkContactResource> workContactResourceMap){
        Map<String, WorkContactResponse> resourceMap = new HashMap<>();
        if (workContactResourceMap != null && !workContactResourceMap.isEmpty()) {
            workContactResourceMap.forEach((key, value) -> {
                WorkContactResponse workContact = new WorkContactResponse();
                workContact.setEmail(toEmailCertifiableFieldResponse(value.getEmail()));
                resourceMap.put(key, workContact);
            });
        }
        return resourceMap;
    }

    @Named("retrieveMailFromWorkContacts")
    default String retrieveMailFromWorkContacts(Map<String, WorkContactResource> map, String userMailUuid){
        return Optional.ofNullable(map)
                .filter(item -> item.containsKey(userMailUuid))
                .map(item -> map.get(userMailUuid))
                .filter(workContactResource -> Objects.nonNull(workContactResource.getEmail()))
                .map(workContactResource -> workContactResource.getEmail().getValue())
                .orElse(null);
    }

    @Named("retrieveMobilePhoneFromWorkContacts")
    default String retrieveMobilePhoneFromWorkContacts(Map<String, WorkContactResource> map, String userMailUuid){
        return Optional.ofNullable(map)
                .filter(item -> item.containsKey(userMailUuid))
                .map(item -> map.get(userMailUuid))
                .filter(workContactResource -> Objects.nonNull(workContactResource.getMobilePhone()))
                .map(workContactResource -> workContactResource.getMobilePhone().getValue())
                .orElse(null);
    }


    @Named("retrieveCertifiedMailFromWorkContacts")
    default CertifiableFieldResponse<String> retrieveCertifiedMailFromWorkContacts(UserResource userResource, String userMailUuid) {
        return Optional.ofNullable(userResource)
                .map(UserResource::getWorkContacts)
                .map(workContacts -> workContacts.get(userMailUuid)).flatMap(resource -> Optional.ofNullable(resource.getEmail())
                        .map(email -> new CertifiableFieldResponse<>(email.getValue(), mapToCertificationEnum(email.getCertification().value()))))
                .orElse(null);
    }

    @Named("retrieveCertifiedMobilePhoneFromWorkContacts")
    default CertifiableFieldResponse<String> retrieveCertifiedMobilePhoneFromWorkContacts(UserResource userResource, String userMailUuid) {
        return Optional.ofNullable(userResource.getWorkContacts())
                .map(workContacts -> workContacts.get(userMailUuid)).flatMap(resource -> Optional.ofNullable(resource.getMobilePhone())
                        .map(mobilePhone -> new CertifiableFieldResponse<>(mobilePhone.getValue(), mapToCertificationEnum(mobilePhone.getCertification().value()))))
                .orElse(null);
    }

    @Named("toFamilyNameCertifiableFieldResponse")
    default CertifiableFieldResponse<String> toFamilyNameCertifiableFieldResponse(FamilyNameCertifiableSchema resource){
        return Optional.ofNullable(resource).map(r -> new CertifiableFieldResponse<>(r.getValue(),
                Optional.ofNullable(r.getCertification()).map(certificationEnum -> mapToCertificationEnum(certificationEnum.value())).orElse(null)))
                .orElse(null);
    }

    @Named("toNameCertifiableFieldResponse")
    default CertifiableFieldResponse<String> toNameCertifiableFieldResponse(NameCertifiableSchema resource) {
        return Optional.ofNullable(resource).map(r -> new CertifiableFieldResponse<>(r.getValue(),
                        Optional.ofNullable(r.getCertification()).map(certificationEnum -> mapToCertificationEnum(certificationEnum.value())).orElse(null)))
                .orElse(null);
    }

    @Named("toEmailCertifiableFieldResponse")
    default CertifiableFieldResponse<String> toEmailCertifiableFieldResponse(EmailCertifiableSchema resource){
        return Optional.ofNullable(resource).map(r -> new CertifiableFieldResponse<>(r.getValue(),
                Optional.ofNullable(r.getCertification()).map(certificationEnum -> mapToCertificationEnum(certificationEnum.value())).orElse(null)))
                .orElse(null);
    }

    default CertificationEnum mapToCertificationEnum(String certificationEnum){
        return switch (certificationEnum) {
            case "SPID" -> CertificationEnum.SPID;
            case "NONE" -> CertificationEnum.NONE;
            default -> throw new InvalidRequestException("Invalid certificationEnum");
        };
    }

    MutableUserFieldsDto toMutableUserFieldsDto(UserResource userResource);

    @Mapping(target = "familyName", expression = "java(toFamilyNameCertifiableStringNotEquals(userResource.getFamilyName(), updateUserRequest.getFamilyName()))")
    @Mapping(target = "name", expression = "java(toNameCertifiableStringNotEquals(userResource.getName(), updateUserRequest.getName()))")
    @Mapping(target = "workContacts", expression = "java(toWorkContact(updateUserRequest.getEmail(), updateUserRequest.getMobilePhone(), idContact))")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    MutableUserFieldsDto toMutableUserFieldsDto(UpdateUserRequest updateUserRequest, UserResource userResource, String idContact);

    @Mapping(source = "user.birthDate", target = "birthDate", qualifiedByName = "toCertifiableLocalDate")
    @Mapping(source = "user.familyName", target = "familyName", qualifiedByName = "toFamilyNameCertifiableString")
    @Mapping(source = "user.name", target = "name", qualifiedByName = "toNameCertifiableString")
    @Mapping(source = "user.fiscalCode", target = "fiscalCode")
    @Mapping(source = "workContactResource", target = "workContacts")
    SaveUserDto toSaveUserDto(CreateUserDto.User user, Map<String, WorkContactResource> workContactResource);

    @Named("toFamilyNameCertifiableStringNotEquals")
    default FamilyNameCertifiableSchema toFamilyNameCertifiableStringNotEquals(FamilyNameCertifiableSchema certifiableString, String value) {
        if(StringUtils.isBlank(value) ||
                Objects.nonNull(certifiableString) && FamilyNameCertifiableSchema.CertificationEnum.SPID.equals(certifiableString.getCertification())){
            return null;
        }

        if(Objects.isNull(certifiableString) || !value.equals(certifiableString.getValue())){
            var certifiableFieldResourceOfstring = new FamilyNameCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(FamilyNameCertifiableSchema.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    @Named("toNameCertifiableStringNotEquals")
    default NameCertifiableSchema toNameCertifiableStringNotEquals(NameCertifiableSchema certifiableString, String value) {
        if(StringUtils.isBlank(value) ||
                Objects.nonNull(certifiableString) && NameCertifiableSchema.CertificationEnum.SPID.equals(certifiableString.getCertification())){
            return null;
        }

        if(Objects.isNull(certifiableString) || !value.equals(certifiableString.getValue())){
            var certifiableFieldResourceOfstring = new NameCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(NameCertifiableSchema.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }
    @Named("toWorkContact")
    default Map<String, WorkContactResource> toWorkContact(String email, String phoneNumber, String idContact){
        if (StringUtils.isNotBlank(idContact)){
            WorkContactResource workContactResource = new WorkContactResource();
            if(StringUtils.isNotBlank(email)){
                workContactResource.setEmail(toMailCertString(email));
            }
            if(StringUtils.isNotBlank(phoneNumber)){
                workContactResource.setMobilePhone(toPhoneCertString(phoneNumber));
            }
            return Map.of(idContact, workContactResource);
        }
        return null;
    }

    @Named("toCertifiableLocalDate")
    default BirthDateCertifiableSchema toLocalTime(String time) {
        if(Objects.isNull(time)) return null;
        var certifiableFieldResourceOfLocalDate = new BirthDateCertifiableSchema();
        certifiableFieldResourceOfLocalDate.setValue(LocalDate.parse(time));
        certifiableFieldResourceOfLocalDate.setCertification(BirthDateCertifiableSchema.CertificationEnum.NONE);
        return certifiableFieldResourceOfLocalDate;
    }


    default EmailCertifiableSchema toMailCertString(String value) {
        if (StringUtils.isNotBlank(value)){
            var certifiableFieldResourceOfstring = new EmailCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(EmailCertifiableSchema.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    default MobilePhoneCertifiableSchema toPhoneCertString(String value){
        if (StringUtils.isNotBlank(value)){
            var certifiableFieldResourceOfstring = new MobilePhoneCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(MobilePhoneCertifiableSchema.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    @Named("toNameCertifiableString")
    default NameCertifiableSchema toNameCertString(String value) {
        if (StringUtils.isNotBlank(value)){
            var certifiableFieldResourceOfstring = new NameCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(NameCertifiableSchema.CertificationEnum.NONE);
            return certifiableFieldResourceOfstring;
        }
        return null;
    }

    @Named("toFamilyNameCertifiableString")
    default FamilyNameCertifiableSchema toFamilyNameCertString(String value) {
        if (StringUtils.isNotBlank(value)){
            var certifiableFieldResourceOfstring = new FamilyNameCertifiableSchema();
            certifiableFieldResourceOfstring.setValue(value);
            certifiableFieldResourceOfstring.setCertification(FamilyNameCertifiableSchema.CertificationEnum.NONE);
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
        if(onboardedProductStateList.isEmpty()) return null;
        return Collections.min(onboardedProductStateList).name();
    }

    @Named("getMaxRole")
    default String getMaxRole(List<OnboardedProduct> onboardedProductList){
        List<PartyRole> partyRoleList = onboardedProductList.stream().map(OnboardedProduct::getRole).toList();
        if(partyRoleList.isEmpty()) return null;
        return Collections.min(partyRoleList).name();
    }

    UserInfoResponse toUserInfoResponse(UserInfo userInfo);
}
