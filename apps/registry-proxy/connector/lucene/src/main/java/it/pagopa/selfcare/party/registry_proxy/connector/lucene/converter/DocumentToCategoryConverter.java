package it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.model.CategoryEntity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;

import java.util.function.Function;

import static it.pagopa.selfcare.party.registry_proxy.connector.model.Category.Field.*;

@Slf4j
public class DocumentToCategoryConverter implements Function<Document, Category> {

    @Override
    public Category apply(Document document) {
        CategoryEntity category = null;
        if (document != null) {
            category = new CategoryEntity();
            category.setCode(document.get(CODE.toString()));
            category.setName(document.get(NAME.toString()));
            category.setKind(document.get(KIND.toString()));
            category.setOrigin(Origin.fromValue(document.get(ORIGIN.toString())));
        }
        return category;
    }

}
