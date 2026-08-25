package it.pagopa.selfcare.commons.web.security;

import static it.pagopa.selfcare.commons.web.handler.RestExceptionsHandler.UNHANDLED_EXCEPTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.web.model.Problem;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AuthenticationConverter authenticationConverter = request -> {
        String jwt = null;
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            jwt = headerAuth.substring(7);
        }

        return new JwtAuthenticationToken(jwt);
    };

    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;


    public JwtAuthenticationFilter(final AuthenticationManager authenticationManager,
                                   final ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        log.trace("doFilterInternal start");
        try {
            try {
                final Authentication authentication = authenticationManager.authenticate(authenticationConverter.convert(request));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                filterChain.doFilter(request, response);
            } catch (AuthenticationException e) {
                log.warn("Cannot set user authentication", e);
                filterChain.doFilter(request, response);
            } catch (final Exception e) {
                log.error(UNHANDLED_EXCEPTION, e);
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                final Problem problem = new Problem(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                response.getOutputStream().print(objectMapper.writeValueAsString(problem));
            }

        } finally {
            SecurityContextHolder.clearContext();
            MDC.clear();
            log.trace("doFilterInternal end");
        }
    }

}
