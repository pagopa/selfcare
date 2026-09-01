package it.pagopa.selfcare.onboarding.connector.rest.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class TenantHeaderInterceptor implements RequestInterceptor {

  private static final String TENANT_HEADER = "X-Tenant-Id";

  @Override
  public void apply(RequestTemplate template) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null
        && ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass())) {
      String tenantId =
          ((ServletRequestAttributes) requestAttributes).getRequest().getHeader(TENANT_HEADER);
      if (StringUtils.hasText(tenantId)) {
        template.header(TENANT_HEADER, tenantId);
      }
    }
  }
}
