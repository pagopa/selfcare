package it.pagopa.selfcare.mscore.web.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptIfTaxCodeDeserializerTest {

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
    void arEncryptsTaxCode(String audience) throws IOException {
        JwtTestSupport.authenticateWithAudience(audience);
        User user = new User();
        user.setId(USER_ID);
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenReturn(user);

        assertEquals(USER_ID, deserialize(TAX_CODE));
    }

    @ParameterizedTest
    @MethodSource("it.pagopa.selfcare.mscore.web.util.JwtTestSupport#pnpgAudiences")
    void pnpgKeepsTaxCodeInClear(String audience) throws IOException {
        JwtTestSupport.authenticateWithAudience(audience);

        assertEquals(TAX_CODE, deserialize(TAX_CODE));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void arKeepsTaxCodeWhenUserIsNotFound() throws IOException {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");
        when(userRegistryConnector.getUserByFiscalCode(TAX_CODE)).thenThrow(mock(FeignException.NotFound.class));

        assertEquals(TAX_CODE, deserialize(TAX_CODE));
    }

    @Test
    void arDoesNotEncryptUuid() throws IOException {
        JwtTestSupport.authenticateWithAudience("api.selfcare.pagopa.it");

        assertEquals(USER_ID, deserialize(USER_ID));
        verifyNoInteractions(userRegistryConnector);
    }

    @Test
    void blankValueIsNull() throws IOException {
        assertNull(deserialize(" "));
        verifyNoInteractions(userRegistryConnector);
    }

    private String deserialize(String value) throws IOException {
        try (JsonParser parser = MAPPER.createParser(MAPPER.writeValueAsString(value))) {
            parser.nextToken();
            return new EncryptIfTaxCodeDeserializer().deserialize(parser, null);
        }
    }
}
