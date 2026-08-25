package it.pagopa.selfcare.commons.web.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class LogRequestInterceptor implements HandlerInterceptor {

    private static final Collection<String> URI_PREFIX_WHITELIST = List.of(
            "/swagger",
            "/v3/api-docs"
    );


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object controller) {
        boolean skipLog = URI_PREFIX_WHITELIST.stream()
                .anyMatch(request.getRequestURI()::startsWith);
        if (!skipLog) {
            log.info("Requested {} {}", request.getMethod(), request.getRequestURI());
        }

        return true;
    }

}
