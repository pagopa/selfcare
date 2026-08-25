package it.pagopa.selfcare.commons.web.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class JwtAuthenticationProviderTest {

    @InjectMocks
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Mock
    private JwtAuthenticationStrategyFactory jwtAuthenticationStrategyFactoryMock;


    @Test
    void authenticate_nullAuth() {
        // given
        Authentication authentication = null;
        // when
        Executable executable = () -> jwtAuthenticationProvider.authenticate(authentication);
        // then
        Assertions.assertThrows(RuntimeException.class, executable);
        verifyNoInteractions(jwtAuthenticationStrategyFactoryMock);
    }


    @Test
    void authenticate() {
        // given
        String token = "token";
        JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(token);
        jwtAuthenticationToken.setDetails("details");
        final JwtAuthenticationStrategy jwtAuthenticationStrategyMock = mock(JwtAuthenticationStrategy.class);
        when(jwtAuthenticationStrategyMock.authenticate(any()))
                .thenReturn(new JwtAuthenticationToken(null, null, null));
        when(jwtAuthenticationStrategyFactoryMock.create(any()))
                .thenReturn(jwtAuthenticationStrategyMock);
        // when
        final Authentication authentication = jwtAuthenticationProvider.authenticate(jwtAuthenticationToken);
        // then
        assertNotNull(authentication);
        assertEquals(jwtAuthenticationToken.getDetails(), authentication.getDetails());
        verify(jwtAuthenticationStrategyFactoryMock, times(1))
                .create(token);
        verify(jwtAuthenticationStrategyMock, times(1))
                .authenticate(jwtAuthenticationToken);
        verifyNoMoreInteractions(jwtAuthenticationStrategyFactoryMock, jwtAuthenticationStrategyMock);
    }


    @Test
    void supports_ko() {
        // given
        Class<?> authentication = Object.class;
        // when
        boolean supported = jwtAuthenticationProvider.supports(authentication);
        // then
        Assertions.assertFalse(supported);
    }


    @Test
    void supports_ok() {
        // given
        Class<?> authentication = JwtAuthenticationToken.class;
        // when
        boolean supported = jwtAuthenticationProvider.supports(authentication);
        // then
        Assertions.assertTrue(supported);
    }

}
