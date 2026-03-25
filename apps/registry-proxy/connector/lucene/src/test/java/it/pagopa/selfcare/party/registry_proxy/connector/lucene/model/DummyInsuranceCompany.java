package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.springframework.test.util.ReflectionTestUtils.invokeGetterMethod;
import static org.springframework.util.StringUtils.capitalize;

public class DummyInsuranceCompany implements InsuranceCompany {

    private final InsuranceCompanyEntity dummyEntity = mockInstance(new InsuranceCompanyEntity());

    public Document toDocument() {
        final Document doc = new Document();
        for (Field field : Field.values()) {
            final String value = String.valueOf(invokeGetterMethod(dummyEntity, "get" + capitalize(field.toString())));
            doc.add(new StringField(field.toString(), value, org.apache.lucene.document.Field.Store.NO));
        }
        return doc;
    }


    @Override
    public String getId() {
        return dummyEntity.getId();
    }

    @Override
    public String getTaxCode() {
        return dummyEntity.getTaxCode();
    }

    @Override
    public String getDescription() {
        return dummyEntity.getDescription();
    }

    @Override
    public String getDigitalAddress() {
        return dummyEntity.getDigitalAddress();
    }

    @Override
    public String getWorkType() {
        return dummyEntity.getWorkType();
    }

    @Override
    public String getRegisterType() {
        return dummyEntity.getRegisterType();
    }

    @Override
    public Origin getOrigin() {
        return dummyEntity.getOrigin();
    }

    @Override
    public String getAddress() {
        return dummyEntity.getAddress();
    }

    @Override
    public String getOriginId() {
        return dummyEntity.getOriginId();
    }

}

