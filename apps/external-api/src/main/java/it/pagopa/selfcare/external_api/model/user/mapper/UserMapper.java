package it.pagopa.selfcare.external_api.model.user.mapper;

import it.pagopa.selfcare.external_api.model.onboarding.User;
import it.pagopa.selfcare.external_api.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.external_api.model.user.SaveUserDto;
import it.pagopa.selfcare.external_api.model.user.WorkContact;

import java.util.Map;

public class UserMapper {

    public static SaveUserDto toSaveUserDto(User model, String institutionId) {
        SaveUserDto resource = null;
        if (model != null) {
            resource = new SaveUserDto();
            resource.setFiscalCode(model.getTaxCode());
            fillMutableUserFieldsDto(model, institutionId, resource);
        }
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
