package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.IamRestClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The HTTP behavior of the IAM client (headers, timeouts, errors) is covered by the runtime tests under {@code runtime}. */
@ExtendWith(MockitoExtension.class)
class IamServiceImplTest {

    private static final String PERMISSION = "Selc:ManageAccountPage";
    private static final String USER_ID = "user-uid";
    private static final String INSTITUTION_ID = "institution-id";
    private static final String PRODUCT_ID = "prod-test";

    @InjectMocks
    private IamServiceImpl iamService;

    @Mock
    private IamRestClient iamRestClient;

    @Test
    void hasIamUserPermission_authorized_returnsTrue() {
        // given
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)
                .await().indefinitely();

        // then
        assertTrue(result);
        verify(response).close();
    }

    @Test
    void hasIamUserPermission_pendingResponse_isNotReadOrClosedUntilItArrives() {
        // given
        AtomicReference<UniEmitter<? super Response>> emitter = new AtomicReference<>();
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().<Response>emitter(emitter::set));
        boolean[] completed = {false};

        // when the permission is requested but the response is still pending
        Uni<Boolean> permission = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID);
        permission.subscribe().with(result -> completed[0] = result);

        // then nothing is read yet
        assertFalse(completed[0]);
        verify(response, never()).hasEntity();
        verify(response, never()).close();

        // when the response arrives
        emitter.get().complete(response);

        // then
        assertTrue(completed[0]);
        verify(response).close();
    }

    @Test
    void hasIamUserPermission_emptyInstitutionQueryIsOmitted() {
        // given
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, null, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, "", PRODUCT_ID).await().indefinitely();

        // then
        assertTrue(result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void hasIamUserPermission_emptyQueriesAreOmitted(String value) {
        // given
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, null, null))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, value, value).await().indefinitely();

        // then
        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "scoped-id"})
    void hasIamUserPermission_nonEmptyQueriesArePreserved(String value) {
        // given
        Response response = responseWithBody(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, value, value))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, value, value).await().indefinitely();

        // then
        assertTrue(result);
    }

    @Test
    void hasIamUserPermission_notAuthorized_returnsFalse() {
        // given
        Response response = responseWithBody(Map.of("hasPermission", false));
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)
                .await().indefinitely();

        // then
        assertFalse(result);
        verify(response).close();
    }

    @Test
    void hasIamUserPermission_bodyWithoutFlag_returnsFalse() {
        // given
        Response response = responseWithBody(Map.of());
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)
                .await().indefinitely();

        // then
        assertFalse(result);
    }

    @Test
    void hasIamUserPermission_emptyBody_returnsFalse() {
        // given
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(false);
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        boolean result = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID)
                .await().indefinitely();

        // then
        assertFalse(result);
        verify(response).close();
    }

    @Test
    void hasIamUserPermission_unreadableBody_isPropagatedAndTheResponseIsClosed() {
        // given
        ProcessingException failure = new ProcessingException("unreadable body");
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(Map.class)).thenThrow(failure);
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().item(response));

        // when
        Uni<Boolean> permission = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID);

        // then
        ProcessingException thrown = assertThrows(ProcessingException.class, () -> permission.await().indefinitely());
        assertSame(failure, thrown);
        verify(response).close();
    }

    @Test
    void hasIamUserPermission_iamFailure_isPropagatedNotTurnedIntoADenial() {
        // given
        WebApplicationException failure = new WebApplicationException(Response.status(403).build());
        when(iamRestClient.hasIAMUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        Uni<Boolean> permission = iamService.hasIamUserPermission(PERMISSION, USER_ID, INSTITUTION_ID, PRODUCT_ID);

        // then
        WebApplicationException thrown = assertThrows(WebApplicationException.class,
                () -> permission.await().indefinitely());
        assertSame(failure, thrown);
        assertEquals(403, thrown.getResponse().getStatus());
    }

    private static Response responseWithBody(Map<String, Object> body) {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(Map.class)).thenReturn(body);
        return response;
    }
}
