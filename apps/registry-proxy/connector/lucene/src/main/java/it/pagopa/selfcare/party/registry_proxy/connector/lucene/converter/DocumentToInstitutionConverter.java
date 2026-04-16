package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InstitutionEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Institution.Field.*;

@Slf4j
public class DocumentToInstitutionConverter implements Function<Document, Institution> {

    @Override
    public Institution apply(Document document) {
        InstitutionEntity institution = null;
        if (document != null) {
            institution = new InstitutionEntity();
            institution.setId(document.get(ID.toString()));
            institution.setOriginId(document.get(ORIGIN_ID.toString()));
            institution.setO(document.get(O.toString()));
            institution.setOu(document.get(OU.toString()));
            institution.setAoo(document.get(AOO.toString()));
            institution.setTaxCode(document.get(TAX_CODE.toString()));
            institution.setCategory(document.get(CATEGORY.toString()));
            institution.setDescription(document.get(DESCRIPTION.toString()));
            institution.setDigitalAddress(document.get(DIGITAL_ADDRESS.toString()));
            institution.setAddress(document.get(ADDRESS.toString()));
            institution.setZipCode(document.get(ZIP_CODE.toString()));
            institution.setOrigin(Origin.fromValue(document.get(ORIGIN.toString())));
            institution.setIstatCode(document.get(ISTAT_CODE.toString()));
        }
        return institution;
    }

}
