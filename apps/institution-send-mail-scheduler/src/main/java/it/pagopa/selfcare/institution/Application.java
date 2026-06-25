package it.pagopa.selfcare.institution;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import it.pagopa.selfcare.institution.service.InstitutionSendMailScheduledServiceImpl;

@QuarkusMain
public class Application implements QuarkusApplication {

    private final InstitutionSendMailScheduledServiceImpl institutionSendMailScheduledServiceImpl;

    public Application(InstitutionSendMailScheduledServiceImpl institutionSendMailScheduledServiceImpl) {
        this.institutionSendMailScheduledServiceImpl = institutionSendMailScheduledServiceImpl;
    }

    @Override
    public int run(String... args){
        return institutionSendMailScheduledServiceImpl.retrieveInstitutionFromMailNotificationAndSendMail()
                .replaceWith(0)
                .await().indefinitely();
    }
}