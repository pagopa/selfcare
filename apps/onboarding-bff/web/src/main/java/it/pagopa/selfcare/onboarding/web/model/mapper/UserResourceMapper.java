package it.pagopa.selfcare.onboarding.web.model.mapper;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.UserInfo;
import it.pagopa.selfcare.onboarding.connector.model.user.CertifiedField;
import it.pagopa.selfcare.onboarding.connector.model.user.WorkContact;
import it.pagopa.selfcare.onboarding.web.model.*;

import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
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
