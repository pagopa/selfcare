package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.mscore.api.MailNotificationConnector;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class MailNotificationConnectorImpl implements MailNotificationConnector {

    private final MailNotificationRepository mailNotificationRepository;

    public MailNotificationConnectorImpl(MailNotificationRepository mailNotificationRepository) {
        this.mailNotificationRepository = mailNotificationRepository;
    }

    @Override
    public boolean addMailNotification(String institutionId, String productId, String digitalAddress, Integer moduleDayOfTheEpoch) {
        final Query query = Query.query(Criteria.where(MailNotificationEntity.FIELD_INSTITUTION_ID).is(institutionId));

        final Update update = new Update()
                .addToSet(MailNotificationEntity.FIELD_PRODUCT_IDS, productId)
                .set(MailNotificationEntity.FIELD_UPDATED_AT, Instant.now())
                .setOnInsert(MailNotificationEntity.FIELD_INSTITUTION_ID, institutionId)
                .setOnInsert(MailNotificationEntity.FIELD_MODULE_DAY_OF_THE_EPOCH, moduleDayOfTheEpoch)
                .setOnInsert(MailNotificationEntity.FIELD_DIGITAL_ADDRESS, digitalAddress)
                .setOnInsert(MailNotificationEntity.FIELD_CREATED_AT, Instant.now());

        log.info("Upserting mail notification for institutionId {} and productId {}", institutionId, productId);
        final UpdateResult updateResult = mailNotificationRepository.upsert(query, update, MailNotificationEntity.class);
        return updateResult.getUpsertedId() != null || (updateResult.getMatchedCount() == 1 && updateResult.getModifiedCount() == 1);
    }

    @Override
    public boolean removeMailNotification(String institutionId, String productId) {
        final Query query = Query.query(Criteria.where(MailNotificationEntity.FIELD_INSTITUTION_ID).is(institutionId));

        final Update update = new Update()
                .pull(MailNotificationEntity.FIELD_PRODUCT_IDS, productId)
                .set(MailNotificationEntity.FIELD_UPDATED_AT, Instant.now());

        final FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().upsert(false).returnNew(true);

        log.info("Removing mail notification for institutionId {} and productId {}", institutionId, productId);
        final Optional<MailNotificationEntity> mailNotificationEntity = Optional.ofNullable(mailNotificationRepository.findAndModify(query, update, findAndModifyOptions, MailNotificationEntity.class));
        return mailNotificationEntity.map(mn -> {
            deleteDocumentIfNoProductsLeft(institutionId);
            return true;
        }).orElseGet(() -> {
            log.warn("Mail notification document not found for institutionId {}", institutionId);
            return false;
        });
    }

    private void deleteDocumentIfNoProductsLeft(String institutionId) {
        final Query query = Query.query(Criteria.where(MailNotificationEntity.FIELD_INSTITUTION_ID).is(institutionId)
                .and(MailNotificationEntity.FIELD_PRODUCT_IDS).size(0));
        mailNotificationRepository.findAndRemove(query, MailNotificationEntity.class);
    }

}
