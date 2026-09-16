package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndProfile;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.PdndSecretValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdndSecretValueResolverTest {

    @Mock
    private PDNDInfoCamereRestClientConfig infoCamereConfig;

    @Mock
    private PDNDInvitaliaRestClientConfig invitaliaConfig;

    private PdndSecretValue infoCamereSecret;
    private PdndSecretValue invitaliaSecret;

    @BeforeEach
    void setUp() {
        infoCamereSecret = PdndSecretValue.builder()
                .clientId("infocamere-client")
                .profile(PdndProfile.SELFCARE)
                .build();
        invitaliaSecret = PdndSecretValue.builder()
                .clientId("invitalia-client")
                .profile(PdndProfile.INVITALIA)
                .build();
        lenient().when(infoCamereConfig.getPdndSecretValue()).thenReturn(infoCamereSecret);
        lenient().when(invitaliaConfig.getPdndSecretValue()).thenReturn(invitaliaSecret);
    }

    private PdndSecretValueResolver resolverWith(String invitaliaProducts) {
        return new PdndSecretValueResolver(infoCamereConfig, invitaliaConfig, invitaliaProducts);
    }

    @Test
    void resolve_returnsInvitaliaSecret_whenProductIsInInvitaliaList() {
        // given
        PdndSecretValueResolver resolver = resolverWith("prod-pn,prod-io");
        // when
        PdndSecretValue result = resolver.resolve("prod-io");
        // then
        assertSame(invitaliaSecret, result);
        verify(infoCamereConfig, never()).getPdndSecretValue();
    }

    @Test
    void resolve_returnsInfoCamereSecret_whenProductIsNotInInvitaliaList() {
        // given
        PdndSecretValueResolver resolver = resolverWith("prod-pn,prod-io");
        // when
        PdndSecretValue result = resolver.resolve("prod-interop");
        // then
        assertSame(infoCamereSecret, result);
        verify(invitaliaConfig, never()).getPdndSecretValue();
    }

    @Test
    void resolve_returnsInfoCamereSecret_whenProductIdIsNull() {
        // given
        PdndSecretValueResolver resolver = resolverWith("prod-pn,prod-io");
        // when
        PdndSecretValue result = resolver.resolve(null);
        // then
        assertSame(infoCamereSecret, result);
        verify(invitaliaConfig, never()).getPdndSecretValue();
    }

    @Test
    void resolve_returnsInfoCamereSecret_whenInvitaliaProductsIsEmpty() {
        // given
        PdndSecretValueResolver resolver = resolverWith("");
        // when
        PdndSecretValue result = resolver.resolve("prod-io");
        // then
        assertSame(infoCamereSecret, result);
        verify(invitaliaConfig, never()).getPdndSecretValue();
    }

    @Test
    void resolve_trimsWhitespaceAndIgnoresEmptyEntries_whenParsingInvitaliaProducts() {
        // given: entries with surrounding spaces and empty tokens
        PdndSecretValueResolver resolver = resolverWith("  prod-pn , , prod-io ,");
        // when / then: trimmed product matches
        assertSame(invitaliaSecret, resolver.resolve("prod-io"));
        assertSame(invitaliaSecret, resolver.resolve("prod-pn"));
        // and an empty productId must not match the ignored empty tokens
        assertSame(infoCamereSecret, resolver.resolve(""));
    }
}

