package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.user.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InfoResourceMapperImplTest {

    private static final String fiscalCode = "DDTPPL89M26F839I";

    @Test
    void testToResource() {
        UserInfoResourceMapperImpl mapper = new UserInfoResourceMapperImpl();
        UserInfoWrapper userWrapper = new UserInfoWrapper();
        User user = new User();
        CertifiedField<String> fieldName = getStringCertifiedField("name");
        user.setName(fieldName);
        CertifiedField<String> fieldEmail = getStringCertifiedField("email");
        user.setEmail(fieldEmail);
        CertifiedField<String> surnameField = getStringCertifiedField("surname");
        user.setFamilyName(surnameField);
        user.setFiscalCode(fiscalCode);
        userWrapper.setUser(user);
        UserInfoResource resource = mapper.toResource(userWrapper);
        assertNotNull(resource);
        assertEquals(fiscalCode, resource.getUser().getFiscalCode());
        assertEquals("name", resource.getUser().getName());
        assertEquals("email", resource.getUser().getEmail());
        assertEquals("surname", resource.getUser().getSurname());
    }

    private CertifiedField<String> getStringCertifiedField(String value) {
        CertifiedField<String> fieldName = new CertifiedField<>();
        fieldName.setValue(value);
        fieldName.setCertification(Certification.SPID);
        return fieldName;
    }
}
