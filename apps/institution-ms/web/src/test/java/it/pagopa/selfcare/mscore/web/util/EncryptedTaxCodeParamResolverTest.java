package it.pagopa.selfcare.mscore.web.util;

import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptedTaxCodeParamResolverTest {

    private static final String TAX_CODE = "RSSMRA80A01H501U";
    private static final String USER_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private UserRegistryConnector userRegistryConnector;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("it.pagopa.selfcare.mscore.web.util.JwtTestSupport#arAudiences")
    void arEncryptsTaxCode(String audience) throws Exception {
        JwtTestSupport.authenticateWithAudience(audience);
        User user = new User();
        user.setId(USER_ID);
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenReturn(user);

        assertEquals(USER_ID, resolve("optional", TAX_CODE));
    }

    @ParameterizedTest
    @MethodSource("it.pagopa.selfcare.mscore.web.util.JwtTestSupport#pnpgAudiences")
    void pnpgKeepsTaxCodeInClear(String audience) throws Exception {
        JwtTestSupport.authenticateWithAudience(audience);

        assertEquals(TAX_CODE, resolve("optional", TAX_CODE));
        verifyNoInteractions(userRegistryConnector);
    }

    /**
     * Known limitation: the rule matches "pnpg" or "imprese.notifichedigitali", so the UAT PNPG session
     * audience is handled as AR. No PNPG UAT caller currently hits the encrypted endpoints with it.
     */
    @Test
    void uatPnpgSessionAudienceIsNotRecognised() throws Exception {
        JwtTestSupport.authenticateWithAudience("imprese.uat.notifichedigitali.it");
        User user = new User();
        user.setId(USER_ID);
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenReturn(user);

        assertEquals(USER_ID, resolve("optional", TAX_CODE));
    }

    @Test
    void onlyFirstAudienceIsConsidered() throws Exception {
        JwtTestSupport.authenticateWithAudience("imprese.notifichedigitali.it", "api.selfcare.pagopa.it");
        assertEquals(TAX_CODE, resolve("optional", TAX_CODE));
        verifyNoInteractions(userRegistryConnector);

        User user = new User();
        user.setId(USER_ID);
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenReturn(user);
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it", "imprese.notifichedigitali.it");
        assertEquals(USER_ID, resolve("optional", TAX_CODE));
    }

    @Test
    void tokenWithoutAudienceIsHandledAsAr() throws Exception {
        JwtTestSupport.authenticateWithAudience();
        User user = new User();
        user.setId(USER_ID);
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenReturn(user);

        assertEquals(USER_ID, resolve("optional", TAX_CODE));
    }

    @Test
    void malformedTokenFails() {
        JwtTestSupport.authenticateWithToken("not-a-jwt");
        assertThrows(RuntimeException.class, () -> resolve("optional", TAX_CODE));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void arKeepsTaxCodeWhenUserIsNotFound() throws Exception {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE))
                .thenThrow(new ResourceNotFoundException("not found", "0000"));

        assertEquals(TAX_CODE, resolve("optional", TAX_CODE));
    }

    @Test
    void arDoesNotEncryptUuidOrNumericValues() throws Exception {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");

        assertEquals(USER_ID, resolve("optional", USER_ID));
        assertEquals("01234567890", resolve("optional", "01234567890"));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void blankRequiredParameterFails() {
        assertThrows(MissingServletRequestParameterException.class, () -> resolve("required", " "));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void blankOptionalParameterReturnsNull() throws Exception {
        assertNull(resolve("optional", null));
        verifyNoInteractions(userRegistryConnector);
    }

    private Object resolve(String methodName, String value) throws Exception {
        MethodParameter parameter = new MethodParameter(
                getClass().getDeclaredMethod(methodName, String.class), 0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.setParameter("taxCode", value);
        }
        return new EncryptedTaxCodeParamResolver(userRegistryConnector)
                .resolveArgument(parameter, null, new ServletWebRequest(request), null);
    }

    @SuppressWarnings("unused")
    private void required(@EncryptedTaxCodeParam(value = "taxCode", required = true) String taxCode) {
    }

    @SuppressWarnings("unused")
    private void optional(@EncryptedTaxCodeParam("taxCode") String taxCode) {
    }
}
