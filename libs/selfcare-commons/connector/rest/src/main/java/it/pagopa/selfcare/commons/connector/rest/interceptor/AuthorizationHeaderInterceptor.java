package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class AuthorizationHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            template.header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", authentication.getCredentials()));

        } else {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null
                    && ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass())) {
                template.header(HttpHeaders.AUTHORIZATION,
                        ((ServletRequestAttributes) requestAttributes)
                                .getRequest()
                                .getHeader(HttpHeaders.AUTHORIZATION));
            }
        }
    }

}
