package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.IamConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamServiceImplTest {

    @InjectMocks
    private IamServiceImpl iamPermissionService;

    @Mock
    private IamConnector iamConnector;

    @Test
    void hasIamUserPermission() {
        String permission = "Selc:AccessProductBackofficeAdmin";
        String userId = "userId";
        String institutionId = "institutionId";
        String productId = "productId";
        when(iamConnector.hasUserPermission(permission, userId, institutionId, productId)).thenReturn(true);

        boolean result = iamPermissionService.hasIamUserPermission(permission, userId, institutionId, productId);

        assertTrue(result);
        Mockito.verify(iamConnector, Mockito.times(1))
                .hasUserPermission(permission, userId, institutionId, productId);
    }
}
