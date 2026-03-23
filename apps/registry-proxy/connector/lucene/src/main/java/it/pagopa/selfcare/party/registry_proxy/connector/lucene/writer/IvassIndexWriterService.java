package it.pagopa.selfcare.party.registry_proxy.connector.lucene.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.lucene.converter.InsuranceCompanyToDocumentConverter;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class IvassIndexWriterService extends IndexWriterServiceTemplate<InsuranceCompany> {

    @Autowired
    public IvassIndexWriterService(@Qualifier("ivassIndexWriterFactory") IndexWriterFactory ivassIndexWriterFactory) {
        super(ivassIndexWriterFactory, new InsuranceCompanyToDocumentConverter());
    }

    @Override
    protected String getId(InsuranceCompany item) {
        return item.getId();
    }

}
