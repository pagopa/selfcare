package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.OneMailConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    private static final String TEMPLATE_ID = "selfcare_user_pt_delegation";
    private static final Map<String, String> TEMPLATE_ATTRIBUTES = Map.of(
            "productName", "product",
            "institutionName", "institution",
            "partnerName", "partner");

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    @Mock
    private OneMailConnector oneMailConnector;

    @Test
    void sendDelegationUserNotification_sendsOneMailForEachRecipient() {
        userNotificationService.sendDelegationUserNotification(List.of("user1@test.it", "user2@test.it"), TEMPLATE_ATTRIBUTES);

        verify(oneMailConnector).sendMail("user1@test.it", TEMPLATE_ID, TEMPLATE_ATTRIBUTES);
        verify(oneMailConnector).sendMail("user2@test.it", TEMPLATE_ID, TEMPLATE_ATTRIBUTES);
        verifyNoMoreInteractions(oneMailConnector);
    }

    @Test
    void sendDelegationUserNotification_continuesWhenARecipientFails() {
        doThrow(new RuntimeException("OneMail error"))
                .when(oneMailConnector).sendMail(eq("user1@test.it"), any(), any());

        assertDoesNotThrow(() -> userNotificationService
                .sendDelegationUserNotification(List.of("user1@test.it", "user2@test.it"), TEMPLATE_ATTRIBUTES));

        verify(oneMailConnector).sendMail("user2@test.it", TEMPLATE_ID, TEMPLATE_ATTRIBUTES);
    }
}
