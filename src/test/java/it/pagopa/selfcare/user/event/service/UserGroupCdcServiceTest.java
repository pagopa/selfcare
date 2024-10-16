package it.pagopa.selfcare.user.event.service;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.user.client.EventHubRestClient;
import it.pagopa.selfcare.user.event.UserGroupCdcService;
import it.pagopa.selfcare.user.event.entity.UserGroupEntity;
import it.pagopa.selfcare.user.event.mapper.UserGroupNotificationMapper;
import it.pagopa.selfcare.user.model.UserGroupNotificationToSend;
import jakarta.inject.Inject;
import org.bson.BsonDocument;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.Set;

import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
public class UserGroupCdcServiceTest {

    @Inject
    UserGroupCdcService userGroupCdcService;

    @RestClient
    @InjectMock
    EventHubRestClient eventHubRestClient;

    @Spy
    UserGroupNotificationMapper userGroupNotificationMapper;

    @Test
    void consumerToSendScUserGroupEventTest() {
        UserGroupEntity userGroupEntity = dummyUserGroup();
        ChangeStreamDocument<UserGroupEntity> document = Mockito.mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(userGroupEntity);
        BsonDocument bsonDocument = Mockito.mock(BsonDocument.class);
        when(document.getDocumentKey()).thenReturn(bsonDocument);

        ArgumentCaptor<UserGroupNotificationToSend> notification = ArgumentCaptor.forClass(UserGroupNotificationToSend.class);
        userGroupCdcService.consumerToSendScUserGroupEvent(document);
        verify(eventHubRestClient, times(1)).
                sendUserGroupMessage(notification.capture());
        Assertions.assertEquals(userGroupEntity.getId(), notification.getValue().getId());
        Assertions.assertEquals(userGroupEntity.getInstitutionId(), notification.getValue().getInstitutionId());
        Assertions.assertEquals(userGroupEntity.getProductId(), notification.getValue().getProductId());
        Assertions.assertEquals(2, notification.getValue().getMembers().size());
    }

    UserGroupEntity dummyUserGroup() {
        UserGroupEntity userGroupEntity = new UserGroupEntity();
        userGroupEntity.setId("UserGroupId");
        userGroupEntity.setInstitutionId("InstitutionId");
        userGroupEntity.setProductId("ProductId");
        userGroupEntity.setMembers(Set.of("Member1", "Member2"));
        return userGroupEntity;
    }
}
