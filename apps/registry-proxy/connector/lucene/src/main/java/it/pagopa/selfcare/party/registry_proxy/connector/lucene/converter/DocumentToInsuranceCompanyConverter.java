package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.InsuranceCompanyEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany.Field.*;

@Slf4j
public class DocumentToInsuranceCompanyConverter implements Function<Document, InsuranceCompany> {

    @Override
    public InsuranceCompany apply(Document document) {
        InsuranceCompanyEntity company = null;
        if (document != null) {
            company = new InsuranceCompanyEntity();
            company.setId(document.get(ID.toString()));
            company.setOriginId(document.get(ORIGIN_ID.toString()));
            company.setTaxCode(StringUtils.isEmpty(document.get(TAX_CODE.toString())) ? null : document.get(TAX_CODE.toString()));
            company.setDescription(document.get(DESCRIPTION.toString()));
            company.setAddress(document.get(ADDRESS.toString()));
            company.setRegisterType(document.get(REGISTER_TYPE.toString()));
            company.setDigitalAddress(document.get(DIGITAL_ADDRESS.toString()));
            company.setOrigin(Origin.IVASS);
        }
        return company;
    }

}
