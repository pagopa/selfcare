package it.pagopa.selfcare.commons.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantContext tenantContext;

    public AuthorizationHeaderInterceptor() {
        this((TenantContext) null);
    }

    @Autowired
    public AuthorizationHeaderInterceptor(ObjectProvider<TenantContext> tenantContextProvider) {
        this(tenantContextProvider.getIfAvailable());
    }

    AuthorizationHeaderInterceptor(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            template.header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", authentication.getCredentials()));
        } else {
            propagateAuthorizationHeader(template);
        }
        propagateTenantHeader(template);
    }

    private void propagateAuthorizationHeader(RequestTemplate template) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null
                && ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass())) {
            template.header(HttpHeaders.AUTHORIZATION,
                    ((ServletRequestAttributes) requestAttributes)
                            .getRequest()
                            .getHeader(HttpHeaders.AUTHORIZATION));
        }
    }

    private void propagateTenantHeader(RequestTemplate template) {
        if (tenantContext != null) {
            String tenantId = tenantContext.requiredTenantId();
            template.removeHeader(TENANT_HEADER);
            template.header(TENANT_HEADER, tenantId);
            return;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null
                && ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass())) {
            String tenantId = ((ServletRequestAttributes) requestAttributes)
                    .getRequest()
                    .getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                template.header(TENANT_HEADER, tenantId);
            }
        }
    }

}
