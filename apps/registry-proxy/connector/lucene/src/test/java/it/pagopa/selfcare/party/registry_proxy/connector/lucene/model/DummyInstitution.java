package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.springframework.test.util.ReflectionTestUtils.invokeGetterMethod;
import static org.springframework.util.StringUtils.capitalize;

public class DummyInstitution implements Institution {

    private final InstitutionEntity dummyEntity = mockInstance(new InstitutionEntity());


    public Document toDocument() {
        final Document doc = new Document();
        for (Field field : Field.values()) {
            final String value = String.valueOf(invokeGetterMethod(dummyEntity, "get" + capitalize(field.toString())));
            doc.add(new StringField(field.toString(), value, Store.NO));
        }
        return doc;
    }


    @Override
    public String getId() {
        return dummyEntity.getId();
    }

    @Override
    public String getOriginId() {
        return dummyEntity.getOriginId();
    }

    @Override
    public String getO() {
        return dummyEntity.getO();
    }

    @Override
    public String getOu() {
        return dummyEntity.getOu();
    }

    @Override
    public String getAoo() {
        return dummyEntity.getAoo();
    }

    @Override
    public String getTaxCode() {
        return dummyEntity.getTaxCode();
    }

    @Override
    public String getCategory() {
        return dummyEntity.getCategory();
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
    public String getAddress() {
        return dummyEntity.getAddress();
    }

    @Override
    public String getZipCode() {
        return dummyEntity.getZipCode();
    }

    @Override
    public Origin getOrigin() {
        return dummyEntity.getOrigin();
    }

    @Override
    public String getIstatCode() {
        return dummyEntity.getIstatCode();
    }


}