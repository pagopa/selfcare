package it.pagopa.selfcare.mscore.connector.dao;

import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface MailNotificationRepository extends MongoRepository<MailNotificationEntity, String>, MongoCustomConnector<MailNotificationEntity, String> {

    @Override
    default Optional<MailNotificationEntity> findById(String id) {
        return find(Query.query(Criteria.where(MailNotificationEntity.FIELD_ID).is(id)), MailNotificationEntity.class).stream().findFirst();
    }

    @Override
    default List<MailNotificationEntity> findAll() {
        return find(new Query(), MailNotificationEntity.class);
    }

    @Override
    default List<MailNotificationEntity> findAllById(Iterable<String> ids) {
        List<String> idList = new ArrayList<>();
        ids.forEach(idList::add);
        return find(Query.query(Criteria.where(MailNotificationEntity.FIELD_ID).in(idList)), MailNotificationEntity.class);
    }

    @Override
    default void deleteById(String id) {
        findAndRemove(Query.query(Criteria.where(MailNotificationEntity.FIELD_ID).is(id)), MailNotificationEntity.class);
    }
}
