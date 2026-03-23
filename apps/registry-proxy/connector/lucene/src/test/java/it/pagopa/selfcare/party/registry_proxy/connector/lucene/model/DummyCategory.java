package it.pagopa.selfcare.party.registry_proxy.connector.lucene.model;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.springframework.test.util.ReflectionTestUtils.invokeGetterMethod;
import static org.springframework.util.StringUtils.capitalize;

public class DummyCategory implements Category {

    private final CategoryEntity dummyEntity = mockInstance(new CategoryEntity());


    public Document toDocument() {
        final Document doc = new Document();
        for (Field field : Field.values()) {
            final String value = String.valueOf(invokeGetterMethod(dummyEntity, "get" + capitalize(field.toString())));
            doc.add(new StringField(field.toString(), value, Store.NO));
        }
        return doc;
    }

    @Override
    public String getCode() {
        return dummyEntity.getCode();
    }

    @Override
    public String getName() {
        return dummyEntity.getName();
    }

    @Override
    public String getKind() {
        return dummyEntity.getKind();
    }

    @Override
    public Origin getOrigin() {
        return dummyEntity.getOrigin();
    }

    @Override
    public String toString() {
        return dummyEntity.toString();
    }

    @Override
    public String getId() {
        return dummyEntity.getId();
    }

}