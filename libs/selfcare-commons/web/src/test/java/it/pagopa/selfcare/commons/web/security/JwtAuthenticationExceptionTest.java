package it.pagopa.selfcare.commons.web.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JwtAuthenticationExceptionTest {

    @Test
    void constructor_withCause() {
        // given
        String msg = "message";
        Throwable cause = new RuntimeException();
        // when
        JwtAuthenticationException exception = new JwtAuthenticationException(msg, cause);
        // then
        Assertions.assertEquals(msg, exception.getMessage());
        Assertions.assertSame(cause, exception.getCause());
    }


    @Test
    void constructor_withoutCause() {
        // given
        String msg = "message";
        // when
        JwtAuthenticationException exception = new JwtAuthenticationException(msg);
        // then
        Assertions.assertEquals(msg, exception.getMessage());
        Assertions.assertNull(exception.getCause());
    }

}