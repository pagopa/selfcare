package it.pagopa.selfcare.user_group.health;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoReadinessIndicatorTest {

    private MongoTemplate mongoTemplate;
    private MongoReadinessIndicator indicator;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        indicator = new MongoReadinessIndicator(
                mongoTemplate,
                "selcUserGroup",
                "mongodb://mongo-primary:27017,mongo-secondary:27017/selcUserGroup");
    }

    @Test
    void returnsUpWhenPingSucceeds() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenReturn(new Document("ok", 1));

        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("mongodb", health.getDetails().get("component"));
        assertEquals("selcUserGroup", health.getDetails().get("database"));
        assertEquals(
                "mongo-primary:27017,mongo-secondary:27017",
                health.getDetails().get("host"));
        assertTrue(health.getDetails().containsKey("latencyMs"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void returnsDownWhenPingFails() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenThrow(new IllegalStateException("no primary"));

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(
                "IllegalStateException: no primary",
                health.getDetails().get("error"));
    }
}
