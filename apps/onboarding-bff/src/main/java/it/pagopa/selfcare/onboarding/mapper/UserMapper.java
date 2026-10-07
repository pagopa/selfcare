package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "jakarta-cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User toUser(UserDto model);

    User toUser(UserDataValidationDto model);

    ManagerInfoResponse toManagerInfoResponse(User user);

    default UserResource toResource(UserInfo model) {
        if (model == null) {
            return null;
        }

        UserResource resource = new UserResource();
        resource.setId(UUID.fromString(model.getId()));
        resource.setRole(model.getRole());
        resource.setStatus(model.getStatus());
        resource.setInstitutionId(UUID.fromString(model.getInstitutionId()));

        if (model.getUser() != null) {
            resource.setName(CertifiedFieldMapper.toValue(model.getUser().getName()));
            resource.setTaxCode(model.getUser().getFiscalCode());
            resource.setSurname(CertifiedFieldMapper.toValue(model.getUser().getFamilyName()));

            Optional.ofNullable(model.getUser().getWorkContacts())
                    .map(map -> map.get(model.getInstitutionId()))
                    .map(WorkContact::getEmail)
                    .map(CertifiedFieldMapper::toValue)
                    .ifPresent(resource::setEmail);
        }

        return resource;
    }

    default String toString(UserTaxCodeDto userTaxCode) {
        return userTaxCode != null ? userTaxCode.getTaxCode() : null;
    }

    static SaveUserDto toSaveUserDto(User model, String institutionId) {
        if (model == null) return null;
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        fillMutableUserFieldsDto(model, institutionId, resource);
        return resource;
    }

    static MutableUserFieldsDto toMutableUserFieldsDto(User model, String institutionId) {
        if (model == null) return null;
        MutableUserFieldsDto resource = new MutableUserFieldsDto();
        fillMutableUserFieldsDto(model, institutionId, resource);
        return resource;
    }

    private static void fillMutableUserFieldsDto(User model, String institutionId, MutableUserFieldsDto resource) {
        resource.setName(CertifiedFieldMapper.map(model.getName()));
        resource.setFamilyName(CertifiedFieldMapper.map(model.getSurname()));
        if (institutionId != null) {
            WorkContact contact = new WorkContact();
            contact.setEmail(CertifiedFieldMapper.map(model.getEmail()));
            resource.setWorkContacts(Map.of(institutionId, contact));
        }
    }
}
