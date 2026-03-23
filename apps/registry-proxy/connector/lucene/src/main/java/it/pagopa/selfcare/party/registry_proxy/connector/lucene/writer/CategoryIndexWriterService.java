package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.CategoryToDocumentConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class CategoryIndexWriterService extends IndexWriterServiceTemplate<Category> {


    @Autowired
    public CategoryIndexWriterService(@Qualifier("categoryIndexWriterFactory") IndexWriterFactory categoryIndexWriterFactory) {
        super(categoryIndexWriterFactory, new CategoryToDocumentConverter());
    }


    @Override
    protected String getId(Category item) {
        return item.getId();
    }

}
