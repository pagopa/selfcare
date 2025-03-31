package it.pagopa.selfcare.auth.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SessionServiceTest {

    @Inject
    SessionService sessionService;

    @Test
    void generateSessionTokenWithValidInputs(){
        var tokenSubscriber = sessionService.generateSessionToken("fiscalNumber", "name", "familyName").subscribe().withSubscriber(UniAssertSubscriber.create());
        tokenSubscriber.assertCompleted();
    }
}
