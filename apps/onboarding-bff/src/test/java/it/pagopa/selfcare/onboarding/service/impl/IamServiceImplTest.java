package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.IamRestClient;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class IamServiceImplTest {

    @InjectMocks
    private IamServiceImpl iamService;

    @Mock
    private IamRestClient iamRestClient;

    @Test
    void hasIamUserPermission_authorized_returnsTrue() {
        // given
        String permission = "MANAGE";
        String userId = "user-uid";
        String institutionId = "";
        String productId = "prod-test";
        Response response = mock(Response.class);

        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(Map.class)).thenReturn(Map.of("hasPermission", true));
        when(iamRestClient.hasIAMUserPermission(permission, userId, institutionId, productId)).thenReturn(response);

        // when
        boolean result = iamService.hasIamUserPermission(permission, userId, institutionId, productId);

        // then
        assertTrue(result);
    }

    @Test
    void hasIamUserPermission_notAuthorized_returnsFalse() {
        // given
        String permission = "MANAGE";
        String userId = "user-uid";
        String institutionId = "";
        String productId = "prod-test";
        Response response = mock(Response.class);

        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(Map.class)).thenReturn(Map.of("hasPermission", false));
        when(iamRestClient.hasIAMUserPermission(permission, userId, institutionId, productId)).thenReturn(response);

        // when
        boolean result = iamService.hasIamUserPermission(permission, userId, institutionId, productId);

        // then
        assertFalse(result);
    }

    @Test
    void hasIamUserPermission_forbiddenResponse_returnsFalse() {
        // given
        String permission = "MANAGE";
        String userId = "user-uid";
        String institutionId = "";
        String productId = "prod-test";
        Response response = mock(Response.class);

        when(response.getStatus()).thenReturn(403);
        when(iamRestClient.hasIAMUserPermission(permission, userId, institutionId, productId)).thenReturn(response);

        // when
        boolean result = iamService.hasIamUserPermission(permission, userId, institutionId, productId);

        // then
        assertFalse(result);
    }
}
