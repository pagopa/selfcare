package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserInfo;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.WorkContact;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.*;

import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface UserResourceMapper {

    User toUser(UserDto model);

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
            resource.setName(model.getUser().getName().getValue());
            resource.setTaxCode(model.getUser().getFiscalCode());
            resource.setSurname(model.getUser().getFamilyName().getValue());

            Optional.ofNullable(model.getUser().getWorkContacts())
                    .map(map -> map.get(model.getInstitutionId()))
                    .map(WorkContact::getEmail)
                    .map(CertifiedField::getValue)
                    .ifPresent(resource::setEmail);
        }

        return resource;
    }

    User toUser(UserDataValidationDto model);
    ManagerInfoResponse toManagerInfoResponse(User user);

    default String toString(UserTaxCodeDto userTaxCode) {
        return userTaxCode != null ? userTaxCode.getTaxCode() : null;
    }


}
