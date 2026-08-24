package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class SpidJwtAuthenticationStrategyTest {

    private static final String MDC_UID = "uid";
    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FISCAL_CODE = "fiscal_number";

    @InjectMocks
    private SpidJwtAuthenticationStrategy spidJwtAuthenticationStrategy;

    @Mock
    private JwtService jwtServiceMock;

    @Mock
    private AuthoritiesRetriever authoritiesRetrieverMock;


    @AfterEach
    void cleanUp() {
        MDC.clear();
    }


    @Test
    void authenticate_nullAuth() {
        // given
        JwtAuthenticationToken authentication = null;
        // when
        Executable executable = () -> spidJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertThrows(RuntimeException.class, executable);
        verifyNoInteractions(jwtServiceMock, authoritiesRetrieverMock);
    }


    @Test
    void authenticate_invalidToken() {
        // given
        String token = "token";
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        doThrow(RuntimeException.class)
                .when(jwtServiceMock)
                .getClaims(Mockito.any());
        // when
        Executable executable = () -> spidJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertThrows(JwtAuthenticationException.class, executable);
        assertNull(MDC.get(MDC_UID));
        verify(jwtServiceMock, times(1))
                .getClaims(token);
        verifyNoMoreInteractions(jwtServiceMock);
        verifyNoInteractions(authoritiesRetrieverMock);
    }


    @Test
    void authenticate_koAuthoritiesRetriever() {
        // given
        String token = "token";
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        when(jwtServiceMock.getClaims(any()))
                .thenReturn(mock(Claims.class));
        doThrow(RuntimeException.class)
                .when(authoritiesRetrieverMock)
                .retrieveAuthorities();
        // when
        Executable executable = () -> spidJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertThrows(AuthoritiesRetrieverException.class, executable);
        assertNull(MDC.get(MDC_UID));
        verify(jwtServiceMock, times(1))
                .getClaims(token);
        verify(authoritiesRetrieverMock, times(1))
                .retrieveAuthorities();
        verifyNoMoreInteractions(jwtServiceMock, authoritiesRetrieverMock);
    }


    @Test
    void authenticate_nullUidAndNullAuthorities() {
        // given
        String token = "token";
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        when(jwtServiceMock.getClaims(any()))
                .thenReturn(mock(Claims.class));
        // when
        Authentication authenticate = spidJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertNull(MDC.get(MDC_UID));
        assertNotNull(authenticate);
        assertEquals(token, authenticate.getCredentials());
        assertEquals(SelfCareUser.class, authenticate.getPrincipal().getClass());
        assertEquals("uid_not_provided", ((SelfCareUser) authenticate.getPrincipal()).getId());
        assertNotNull(authenticate.getAuthorities());
        assertTrue(authenticate.getAuthorities().isEmpty());
        verify(jwtServiceMock, times(1))
                .getClaims(token);
        verify(authoritiesRetrieverMock, times(1))
                .retrieveAuthorities();
        verifyNoMoreInteractions(jwtServiceMock, authoritiesRetrieverMock);
    }


    @Test
    void authenticate() {
        // given
        String token = "token";
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(token);
        String uid = "uid";
        String email = "email@prova.com";
        String fiscalCode = "fiscalCode";
        when(jwtServiceMock.getClaims(any()))
                .thenReturn(new DefaultClaims(Map.of(CLAIM_UID, uid, CLAIM_EMAIL, email, CLAIM_FISCAL_CODE, fiscalCode)));
        String role = "role";
        when(authoritiesRetrieverMock.retrieveAuthorities())
                .thenReturn(List.of(new SimpleGrantedAuthority(role), new SimpleGrantedAuthority(role), new SimpleGrantedAuthority(role)));
        // when
        Authentication authenticate = spidJwtAuthenticationStrategy.authenticate(authentication);
        // then
        assertEquals(uid, MDC.get(MDC_UID));
        assertNotNull(authenticate);
        assertEquals(token, authenticate.getCredentials());
        assertEquals(SelfCareUser.class, authenticate.getPrincipal().getClass());
        assertEquals(uid, ((SelfCareUser) authenticate.getPrincipal()).getId());
        assertEquals(email, ((SelfCareUser) authenticate.getPrincipal()).getEmail());
        assertEquals(fiscalCode, ((SelfCareUser) authenticate.getPrincipal()).getFiscalCode());
        assertNotNull(authenticate.getAuthorities());
        assertEquals(3, authenticate.getAuthorities().size());
        authenticate.getAuthorities().forEach(grantedAuthority -> assertEquals(role, grantedAuthority.getAuthority()));
        verify(jwtServiceMock, times(1))
                .getClaims(token);
        verify(authoritiesRetrieverMock, times(1))
                .retrieveAuthorities();
        verifyNoMoreInteractions(jwtServiceMock, authoritiesRetrieverMock);
    }

}