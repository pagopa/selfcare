package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RegistryProxyMapperTest {

    @Test
    void searchResultsKeepEveryFieldAndCount() throws Exception {
        var json = new ObjectMapper();
        var input = json.readTree("""
                {"count":1,"items":[{"id":"id","originId":"origin","o":"o","ou":"ou","aoo":"aoo",
                "taxCode":"tax","category":"L6","description":"desc","digitalAddress":"mail@example.test",
                "address":"address","zipCode":"00100","origin":"IPA","istatCode":"istat"}]}
                """);
        var mapper = Mappers.getMapper(RegistryProxyMapper.class);
        var response = json.treeToValue(input, IpaInstitutionsSearchResponse.class);
        var resource = mapper.toResource(mapper.toIpaInstitutionsSearchResult(response));

        assertEquals(input, json.readTree(json.writeValueAsString(resource)));
        assertNull(mapper.toIpaInstitutionsSearchResult(null));
    }

    @Test
    void certifiedFieldsKeepTheirValueAndDefaultCertification() {
        assertNull(CertifiedFieldMapper.map(null));
        assertNull(CertifiedFieldMapper.toValue(null));
        var field = CertifiedFieldMapper.map("name");
        assertEquals("name", CertifiedFieldMapper.toValue(field));
        assertEquals(it.pagopa.selfcare.onboarding.client.model.Certification.NONE, field.getCertification());
    }
}
