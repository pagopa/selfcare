package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MailNotificationRepository extends MongoRepository<MailNotificationEntity, String>, MongoCustomConnector {

}
