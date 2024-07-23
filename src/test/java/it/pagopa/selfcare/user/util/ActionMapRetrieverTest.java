package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ActionMapRetrieverTest {

    @Test
    void retrieveActionsMap_returnsCorrectActionsMap() {

        ActionMapRetriever actionMapRetriever = new ActionMapRetriever();

        // Then
        assertNotNull(actionMapRetriever.getUserActionsMap());
        assertEquals(5, actionMapRetriever.getUserActionsMap().size());
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.MANAGER));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.DELEGATE));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.ADMIN_EA));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.SUB_DELEGATE));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.OPERATOR));

    }
}
