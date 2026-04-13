package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.client.model.SaveUserDto;
import it.pagopa.selfcare.onboarding.client.model.WorkContact;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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


    public static MutableUserFieldsDto toMutableUserFieldsDto(User model, String institutionId) {
        MutableUserFieldsDto resource = null;
        if (model != null) {
            resource = new MutableUserFieldsDto();
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
