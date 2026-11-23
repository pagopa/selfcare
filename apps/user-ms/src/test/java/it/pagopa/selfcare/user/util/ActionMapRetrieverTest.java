package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ActionMapRetrieverTest {

    @Test
    void retrieveActionsMap_returnsCorrectActionsMap() {

        ActionMapRetriever actionMapRetriever = new ActionMapRetriever();

        // Then
        assertNotNull(actionMapRetriever.getUserActionsMap());
        assertEquals(5, actionMapRetriever.getUserActionsMap().size());
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.MANAGER.name()));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.DELEGATE.name()));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.ADMIN_EA.name()));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.SUB_DELEGATE.name()));
        assertTrue(actionMapRetriever.getUserActionsMap().containsKey(PartyRole.OPERATOR.name()));

    }

    @Test
    void retrieveActionsMap_adminEaHasNoViewContractAction() {

        ActionMapRetriever actionMapRetriever = new ActionMapRetriever();

        actionMapRetriever.getUserActionsMap().get(PartyRole.ADMIN_EA.name())
                .forEach((productId, actions) -> assertFalse(actions.contains("Selc:ViewContract"),
                        "ADMIN_EA must not have Selc:ViewContract for " + productId));
    }

    @Test
    void retrieveActionsMap_updateContractOnlyForPagopaManagerDelegateAndSubDelegate() {

        ActionMapRetriever actionMapRetriever = new ActionMapRetriever();
        Set<String> allowedRoles = Set.of(PartyRole.MANAGER.name(), PartyRole.DELEGATE.name(), PartyRole.SUB_DELEGATE.name());

        actionMapRetriever.getUserActionsMap().forEach((role, productActions) ->
                productActions.forEach((productId, actions) -> assertEquals(
                        allowedRoles.contains(role) && "prod-pagopa".equals(productId),
                        actions.contains("Selc:UpdateContract"),
                        "Unexpected Selc:UpdateContract mapping for role " + role + " and product " + productId)));
    }
}
