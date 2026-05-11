package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.user.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapperCustom {

    public static UserResource toUserResource(UserInfo model, String productId) {
        UserResource resource = null;
        if (model != null) {
            resource = new UserResource();
            resource.setId(UUID.fromString(model.getId()));
            if (model.getUser() != null) {
                resource.setName(CertifiedFieldMapper.toValue(model.getUser().getName()));
                resource.setSurname(CertifiedFieldMapper.toValue(model.getUser().getFamilyName()));
                resource.setRole(model.getPartyRole());
                resource.setFiscalCode(model.getUser().getFiscalCode());
                resource.setRoles(model.getProducts().entrySet().stream()
                        .filter(entry -> entry.getKey().equals(productId))
                        .map(Map.Entry::getValue)
                        .map(ProductInfoDetails::getRoleInfos)
                        .flatMap(Collection::stream)
                        .map(RoleInfo::getRole)
                        .collect(Collectors.toList()));
                Optional.ofNullable(model.getUser().getWorkContacts())
                        .filter(map -> map.containsKey(model.getUserUuidMail()))
                        .map(map -> map.get(model.getUserUuidMail()))
                        .map(WorkContact::getEmail)
                        .map(CertifiedFieldMapper::toValue)
                        .ifPresent(resource::setEmail);
            }
        }
        return resource;
    }

    public static it.pagopa.selfcare.external_api.model.onboarding.User toUser(UserDto model) {
        it.pagopa.selfcare.external_api.model.onboarding.User resource = null;
        if (model != null) {
            resource = new it.pagopa.selfcare.external_api.model.onboarding.User();
            resource.setRole(model.getRole());
            resource.setEmail(model.getEmail());
            resource.setName(model.getName());
            resource.setSurname(model.getSurname());
            resource.setProductRole(model.getProductRole());
            resource.setTaxCode(model.getTaxCode());
        }
        return resource;
    }


    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class CertifiedFieldMapper {

        static String toValue(CertifiedField<String> certifiedField) {
            return certifiedField != null ? certifiedField.getValue() : null;
        }

    }

}
