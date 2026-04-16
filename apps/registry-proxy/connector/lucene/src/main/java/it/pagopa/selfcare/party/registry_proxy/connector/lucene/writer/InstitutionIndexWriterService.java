package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.InstitutionToDocumentConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class InstitutionIndexWriterService extends IndexWriterServiceTemplate<Institution> {


    @Autowired
    public InstitutionIndexWriterService(@Qualifier("institutionIndexWriterFactory") IndexWriterFactory institutionIndexWriterFactory) {
        super(institutionIndexWriterFactory, new InstitutionToDocumentConverter());
    }


    @Override
    protected String getId(Institution item) {
        return item.getId();
    }

}
