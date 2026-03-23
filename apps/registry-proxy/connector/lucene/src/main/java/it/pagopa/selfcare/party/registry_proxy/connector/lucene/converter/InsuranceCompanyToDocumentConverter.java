package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany.Field.*;

@Slf4j
public class InsuranceCompanyToDocumentConverter implements Function<InsuranceCompany, Document> {

    @Override
    public Document apply(InsuranceCompany company) {
        Document doc = null;
        if (company != null) {
            doc = new Document();
            doc.add(new StringField(Entity.ENTITY_TYPE.toString(), Entity.INSURANCE_COMPANY.toString(), Field.Store.YES));
            doc.add(new StringField(ID.toString(), company.getId(), Field.Store.YES));
            doc.add(new StoredField(ORIGIN.toString(), company.getOrigin().toString()));
            doc.add(new StringField(ORIGIN_ID.toString(), company.getOriginId(), Field.Store.YES));
            doc.add(new SortedDocValuesField(DESCRIPTION.toString(), new BytesRef(company.getDescription())));
            doc.add(new TextField(DESCRIPTION.toString(), company.getDescription(), Field.Store.YES));
            doc.add(new StringField(TAX_CODE.toString(), company.getTaxCode(), Field.Store.YES));
            doc.add(new StoredField(DIGITAL_ADDRESS.toString(), company.getDigitalAddress()));
            doc.add(new StoredField(ADDRESS.toString(), String.valueOf(company.getAddress())));
            doc.add(new StoredField(REGISTER_TYPE.toString(), String.valueOf(company.getRegisterType())));
        }
        return doc;
    }

}
