package it.pagopa.selfcare.onboarding.service.helper;

import static it.pagopa.selfcare.onboarding.service.helper.UserRegistryHelper.USERS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

class UserRegistryHelperTest {

    private UserApi userRegistryApi;
    private UserRegistryHelper userRegistryHelper;

    @BeforeEach
    void setUp() throws Exception {
        userRegistryApi = mock(UserApi.class);
        userRegistryHelper = new UserRegistryHelper();
        // inject mock via reflection
        var field = UserRegistryHelper.class.getDeclaredField("userRegistryApi");
        field.setAccessible(true);
        field.set(userRegistryHelper, userRegistryApi);
    }

    // --- isPersonalFiscalCode tests ---

    @ParameterizedTest
    @ValueSource(strings = {"PLTGMR96D20H224Z", "RSSMRA85M01H501Z", "VRDLGI80A01F205X"})
    void isPersonalFiscalCode_validPersonalCF_returnsTrue(String cf) {
        assertTrue(UserRegistryHelper.isPersonalFiscalCode(cf));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000001", "12345678901", "0000000000123456"})
    void isPersonalFiscalCode_numericTaxCode_returnsFalse(String cf) {
        assertFalse(UserRegistryHelper.isPersonalFiscalCode(cf));
    }

    @Test
    void isPersonalFiscalCode_null_returnsFalse() {
        assertFalse(UserRegistryHelper.isPersonalFiscalCode(null));
    }

    @Test
    void isPersonalFiscalCode_wrongLength_returnsFalse() {
        assertFalse(UserRegistryHelper.isPersonalFiscalCode("ABC"));
    }

    // --- resolveTaxCodeForQuery tests ---

    @Test
    void resolveTaxCodeForQuery_numericTaxCode_returnsDirectly() {
        String taxCode = "00000000001";

        String result = userRegistryHelper.resolveTaxCodeForQuery(taxCode)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted().awaitItem().getItem();

        assertEquals(taxCode, result);
        verify(userRegistryApi, never()).searchUsingPOST(any(), any());
    }

    @Test
    void resolveTaxCodeForQuery_personalFiscalCode_userFound_returnsUuid() {
        String personalCF = "PLTGMR96D20H224Z";
        UUID userId = UUID.randomUUID();
        UserResource userResource = new UserResource();
        userResource.setId(userId);

        when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        String result = userRegistryHelper.resolveTaxCodeForQuery(personalCF)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted().awaitItem().getItem();

        assertEquals(userId.toString(), result);
        verify(userRegistryApi).searchUsingPOST(eq(USERS_FIELD_LIST), any());
    }

    @Test
    void resolveTaxCodeForQuery_personalFiscalCode_userNotFound_returnsNull() {
        String personalCF = "RSSMRA85M01H501Z";

        WebApplicationException notFound = mock(WebApplicationException.class);
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(404);
        when(notFound.getResponse()).thenReturn(mockResponse);
        when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any()))
                .thenReturn(Uni.createFrom().failure(notFound));

        String result = userRegistryHelper.resolveTaxCodeForQuery(personalCF)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted().awaitItem().getItem();

        assertNull(result);
    }

    @Test
    void resolveTaxCodeForQuery_personalFiscalCode_pdvError_propagatesFailure() {
        String personalCF = "VRDLGI80A01F205X";

        WebApplicationException serverError = mock(WebApplicationException.class);
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(500);
        when(serverError.getResponse()).thenReturn(mockResponse);
        when(userRegistryApi.searchUsingPOST(eq(USERS_FIELD_LIST), any()))
                .thenReturn(Uni.createFrom().failure(serverError));

        userRegistryHelper.resolveTaxCodeForQuery(personalCF)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailed();
    }
}

