package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field.*;

@Slf4j
public class CategoryToDocumentConverter implements Function<Category, Document> {

    @Override
    public Document apply(Category category) {
        Document doc = null;
        if (category != null) {
            doc = new Document();
            doc.add(new StringField(Entity.ENTITY_TYPE.toString(), Entity.CATEGORY.toString(), Field.Store.YES));
            doc.add(new StringField(ID.toString(), category.getId(), Field.Store.YES));
            doc.add(new StringField(CODE.toString(), category.getCode(), Field.Store.YES));
            doc.add(new StoredField(NAME.toString(), category.getName()));
            doc.add(new StoredField(KIND.toString(), category.getKind()));
            doc.add(new StringField(ORIGIN.toString(), category.getOrigin().toString(), Field.Store.YES));
        }
        return doc;
    }

}
