package it.pagopa.selfcare.mscore.connector.dao;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.mscore.connector.dao.model.MailNotificationEntity;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailNotificationConnectorImplTest {

    @InjectMocks
    private MailNotificationConnectorImpl mailNotificationConnector;

    @Mock
    private MailNotificationRepository repository;

    @Test
    void addMailNotification_insert() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String digitalAddress = "test@test.com";
        final Integer moduleDayOfTheEpoch = 17;

        final ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        final ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);
        final UpdateResult updateResult = new UpdateResult() {
            @Override
            public boolean wasAcknowledged() {
                return true;
            }

            @Override
            public long getMatchedCount() {
                return 1;
            }

            @Override
            public long getModifiedCount() {
                return 1;
            }

            @Override
            public BsonValue getUpsertedId() {
                return new BsonObjectId(new ObjectId("5f5b3b3b1d1f1f1f1f1f1f1f"));
            }
        };

        when(repository.upsert(any(), any(), eq(MailNotificationEntity.class))).thenReturn(updateResult);

        final boolean result = mailNotificationConnector.addMailNotification(institutionId, productId, digitalAddress, moduleDayOfTheEpoch);
        Assertions.assertTrue(result);

        verify(repository, times(1)).upsert(queryCaptor.capture(), updateCaptor.capture(), eq(MailNotificationEntity.class));
        final Query query = queryCaptor.getValue();
        Assertions.assertEquals("institutionId", query.getQueryObject().get("institutionId", String.class));

        final Document updateDocument = updateCaptor.getValue().getUpdateObject();
        Assertions.assertEquals("productId", updateDocument.get("$addToSet", Document.class).get("productIds", String.class));
        Assertions.assertNotNull(updateDocument.get("$set", Document.class).get("updatedAt", Instant.class));
        Assertions.assertEquals("institutionId", updateDocument.get("$setOnInsert", Document.class).get("institutionId", String.class));
        Assertions.assertEquals(17, updateDocument.get("$setOnInsert", Document.class).get("moduleDayOfTheEpoch", Integer.class));
        Assertions.assertEquals("test@test.com", updateDocument.get("$setOnInsert", Document.class).get("digitalAddress", String.class));
        Assertions.assertNotNull(updateDocument.get("$setOnInsert", Document.class).get("createdAt", Instant.class));
    }

    @Test
    void addMailNotification_update() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String digitalAddress = "test@test.com";
        final Integer moduleDayOfTheEpoch = 17;

        final UpdateResult updateResult = new UpdateResult() {
            @Override
            public boolean wasAcknowledged() {
                return true;
            }

            @Override
            public long getMatchedCount() {
                return 1;
            }

            @Override
            public long getModifiedCount() {
                return 1;
            }

            @Override
            public BsonValue getUpsertedId() {
                return null;
            }
        };

        when(repository.upsert(any(), any(), eq(MailNotificationEntity.class))).thenReturn(updateResult);

        final boolean result = mailNotificationConnector.addMailNotification(institutionId, productId, digitalAddress, moduleDayOfTheEpoch);
        Assertions.assertTrue(result);
    }

    @Test
    void addMailNotification_fail() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String digitalAddress = "test@test.com";
        final Integer moduleDayOfTheEpoch = 17;

        final UpdateResult updateResult = new UpdateResult() {
            @Override
            public boolean wasAcknowledged() {
                return true;
            }

            @Override
            public long getMatchedCount() {
                return 0;
            }

            @Override
            public long getModifiedCount() {
                return 0;
            }

            @Override
            public BsonValue getUpsertedId() {
                return null;
            }
        };

        when(repository.upsert(any(), any(), eq(MailNotificationEntity.class))).thenReturn(updateResult);

        final boolean result = mailNotificationConnector.addMailNotification(institutionId, productId, digitalAddress, moduleDayOfTheEpoch);
        Assertions.assertFalse(result);
    }

    @Test
    void removeMailNotification() {
        final String institutionId = "institutionId";
        final String productId = "productId";

        final ArgumentCaptor<Query> updateQueryCaptor = ArgumentCaptor.forClass(Query.class);
        final ArgumentCaptor<Query> deleteQueryCaptor = ArgumentCaptor.forClass(Query.class);
        final ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

        final MailNotificationEntity mailNotificationEntity = new MailNotificationEntity();
        mailNotificationEntity.setId(new ObjectId("5f5b3b3b1d1f1f1f1f1f1f1f"));
        mailNotificationEntity.setInstitutionId("institutionId");
        mailNotificationEntity.setProductIds(Set.of("altProductId"));
        mailNotificationEntity.setDigitalAddress("test@test.com");
        mailNotificationEntity.setModuleDayOfTheEpoch(17);
        mailNotificationEntity.setCreatedAt(Instant.now());
        mailNotificationEntity.setUpdatedAt(Instant.now());

        when(repository.findAndModify(any(), any(), any(), eq(MailNotificationEntity.class))).thenReturn(mailNotificationEntity);
        when(repository.findAndRemove(any(), eq(MailNotificationEntity.class))).thenReturn(mailNotificationEntity);

        final boolean result = mailNotificationConnector.removeMailNotification(institutionId, productId);
        Assertions.assertTrue(result);

        verify(repository, times(1)).findAndModify(updateQueryCaptor.capture(), updateCaptor.capture(), any(), eq(MailNotificationEntity.class));
        final Query updateQuery = updateQueryCaptor.getValue();
        Assertions.assertEquals("institutionId", updateQuery.getQueryObject().get("institutionId", String.class));
        final Update update = updateCaptor.getValue();
        Assertions.assertEquals("productId", update.getUpdateObject().get("$pull", Document.class).get("productIds", String.class));
        Assertions.assertNotNull(update.getUpdateObject().get("$set", Document.class).get("updatedAt", Instant.class));

        verify(repository, times(1)).findAndRemove(deleteQueryCaptor.capture(), eq(MailNotificationEntity.class));
        final Query deleteQuery = deleteQueryCaptor.getValue();
        Assertions.assertEquals("institutionId", deleteQuery.getQueryObject().get("institutionId", String.class));
        Assertions.assertEquals(0, deleteQuery.getQueryObject().get("productIds", Document.class).get("$size", Integer.class));
    }

    @Test
    void removeMailNotification_fail() {
        final String institutionId = "institutionId";
        final String productId = "productId";

        when(repository.findAndModify(any(), any(), any(), eq(MailNotificationEntity.class))).thenReturn(null);

        final boolean result = mailNotificationConnector.removeMailNotification(institutionId, productId);
        Assertions.assertFalse(result);
    }

}
