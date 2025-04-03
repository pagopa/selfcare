package it.pagopa.selfcare.auth.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.auth.exception.InternalException;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserId;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SessionServiceTest {

  @Inject SessionService sessionService;

  @RestClient @InjectMock UserApi userRegistryApi;

  @Test
  void generateSessionTokenWithValidInputs() {
    when(userRegistryApi.saveUsingPATCH(any()))
        .thenReturn(Uni.createFrom().item(UserId.builder().id(UUID.randomUUID()).build()));
    sessionService
        .generateSessionToken("fiscalNumber", "name", "familyName")
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .assertCompleted();
  }

  @Test
  void failOnUserRegistryFailure() {
    when(userRegistryApi.saveUsingPATCH(any()))
            .thenReturn(Uni.createFrom().failure(new WebApplicationException("Cannot invoke pdv")));
    sessionService
            .generateSessionToken("fiscalNumber", "name", "familyName")
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create())
            .assertFailed().assertFailedWith(InternalException.class, "Cannot invoke pdv");
  }
}
