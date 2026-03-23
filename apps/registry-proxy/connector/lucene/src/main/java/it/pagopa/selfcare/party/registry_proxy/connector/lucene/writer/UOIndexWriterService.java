package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.UOToDocumentConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class UOIndexWriterService extends IndexWriterServiceTemplate<UO> {


    @Autowired
    public UOIndexWriterService(@Qualifier("uoIndexWriterFactory") IndexWriterFactory uoIndexWriterFactory) {
        super(uoIndexWriterFactory, new UOToDocumentConverter());
    }


    @Override
    protected String getId(UO item) {
        return item.getId();
    }

}
