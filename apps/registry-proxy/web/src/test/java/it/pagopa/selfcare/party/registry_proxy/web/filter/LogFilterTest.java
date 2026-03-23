package it.pagopa.selfcare.party.registry_proxy.web.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {LogFilter.class})
@ExtendWith(SpringExtension.class)
class LogFilterTest {
    @Autowired
    private LogFilter logFilter;

    /**
     * Method under test: {@link LogFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     */
    @Test
    void testDoFilter4() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(any(), any());
        logFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(any(), any());
    }

    /**
     * Method under test: {@link LogFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     */
    @Test
    void testDoFilter5() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Response response = new Response();
        FilterChain filterChain = mock(FilterChain.class);
        doThrow(new UnsupportedEncodingException()).when(filterChain)
                .doFilter(any(), any());
        assertThrows(UnsupportedEncodingException.class, () -> logFilter.doFilter(request, response, filterChain));
        verify(filterChain).doFilter(any(), any());
    }
}

