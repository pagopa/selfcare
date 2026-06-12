package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.UpdateUserDto;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UsersCountResponse;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import jakarta.validation.ValidationException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface UserMapperV2 {

    @Mapping(target = "name", expression = "java(toCertifiedFieldResource(model.getName()))")
    @Mapping(target = "familyName", expression = "java(toCertifiedFieldResource(model.getFamilyName()))")
    @Mapping(target = "email", expression = "java(toCertifiedFieldResource(model.getEmail()))")
    @Mapping(target = "mobilePhone", expression = "java(toCertifiedFieldResource(model.getMobilePhone()))")
    UserResource toUserResource(User model);

    UserCountResource toUserCountResource(UsersCountResponse userCount);

    UpdateUserRequestDto fromUpdateUser(UpdateUserDto userDto);

    @Named("toCertifiedFieldResource")
    default CertifiedFieldResource<String> toCertifiedFieldResource(CertifiedField<String> certifiedField){
        CertifiedFieldResource<String> resource = null;
        if (certifiedField!= null){
            resource = new CertifiedFieldResource<>();
            resource.setValue(certifiedField.getValue());
            resource.setCertified(Certification.isCertified(certifiedField.getCertification()));
        }
        return resource;
    }

    @Named("mapCertifiedField")
    default <T> CertifiedField<T> mapCertifiedField(T certifiedField) {
        CertifiedField<T> resource = null;
        if (certifiedField != null) {
            resource = new CertifiedField<>();
            resource.setValue(certifiedField);
            resource.setCertification(Certification.NONE);
        }
        return resource;
    }

    @Named("getEmail")
    default Map<String, WorkContact> getEmail(UpdateUserDto userDto, String institutionId) {
        if (Objects.nonNull(userDto) && Objects.nonNull(institutionId)) {
            WorkContact contact = new WorkContact();
            contact.setEmail(this.mapCertifiedField(userDto.getEmail()));
            return Map.of(institutionId, contact);
        }
        return Map.of();
    }

    @Mapping(target = "role", expression = "java(retrievePartyRole(user.getRole()))")
    UserToCreate toUserToCreate(CreateUserDto user);

    @Named("retrievePartyRole")
    default PartyRole retrievePartyRole(String role) {
        try {
            if (StringUtils.isNotBlank(role)) {
                return PartyRole.valueOf(role);
            }
            return null;
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role: " + role + ". Allowed values are: " + Arrays.toString(PartyRole.values()));
        }
    }
}
