package it.pagopa.selfcare.party.registry_proxy.web.filter;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
public class LogFilter implements Filter {

  private final TenantContext tenantContext;

  public LogFilter(TenantContext tenantContext) {
    this.tenantContext = tenantContext;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;

    final String httpUri = httpServletRequest.getRequestURI();

    if (httpUri.startsWith("/actuator/health")) {
      log.trace("request to health-check actuator");
      chain.doFilter(httpServletRequest, httpServletResponse);
      return;
    }

    final String httpMethod = httpServletRequest.getMethod();
    long startTime = System.currentTimeMillis();
    try {
      chain.doFilter(httpServletRequest, httpServletResponse);
    } finally {
      log.info("Request route: {} - tenant: {} - method: {} - status: {} - timelapse: {}ms",
          httpServletRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE),
          tenantContext.tenantId().orElse("unresolved"), httpMethod,
          httpServletResponse.getStatus(), System.currentTimeMillis() - startTime);
    }
  }
}
