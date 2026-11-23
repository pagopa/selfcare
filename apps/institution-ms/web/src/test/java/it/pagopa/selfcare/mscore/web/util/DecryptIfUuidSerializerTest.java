package it.pagopa.selfcare.mscore.web.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecryptIfUuidSerializerTest {

    private static final String TAX_CODE = "RSSMRA80A01H501U";
    private static final String USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private UserRegistryConnector userRegistryConnector;

    private MockedStatic<SpringContext> springContext;

    @BeforeEach
    void mockSpringContext() {
        springContext = mockStatic(SpringContext.class);
        springContext.when(() -> SpringContext.getBean(UserRegistryConnector.class)).thenReturn(userRegistryConnector);
    }

    @AfterEach
    void cleanUp() {
        springContext.close();
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("it.pagopa.selfcare.mscore.web.util.JwtTestSupport#arAudiences")
    void arDecryptsUuid(String audience) throws IOException {
        JwtTestSupport.authenticateWithAudience(audience);
        User user = new User();
        user.setFiscalCode(TAX_CODE);
        when(userRegistryConnector.getUserByInternalIdWithFiscalCode(USER_ID)).thenReturn(user);

        assertEquals("\"" + TAX_CODE + "\"", serialize(USER_ID));
    }

    @ParameterizedTest
    @MethodSource("it.pagopa.selfcare.mscore.web.util.JwtTestSupport#pnpgAudiences")
    void pnpgKeepsValueAsIs(String audience) throws IOException {
        JwtTestSupport.authenticateWithAudience(audience);

        assertEquals("\"" + USER_ID + "\"", serialize(USER_ID));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void arKeepsUuidWhenUserIsNotFound() throws IOException {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");
        when(userRegistryConnector.getUserByInternalIdWithFiscalCode(USER_ID))
                .thenThrow(new ResourceNotFoundException("not found", "0000"));

        assertEquals("\"" + USER_ID + "\"", serialize(USER_ID));
    }

    @Test
    void arDoesNotDecryptTaxCode() throws IOException {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");

        assertEquals("\"" + TAX_CODE + "\"", serialize(TAX_CODE));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void blankValueIsNull() throws IOException {
        assertEquals("null", serialize(" "));
        verifyNoInteractions(userRegistryConnector);
    }

    private String serialize(String value) throws IOException {
        StringWriter output = new StringWriter();
        try (JsonGenerator generator = MAPPER.createGenerator(output)) {
            new DecryptIfUuidSerializer().serialize(value, generator, null);
        }
        return output.toString();
    }
}
