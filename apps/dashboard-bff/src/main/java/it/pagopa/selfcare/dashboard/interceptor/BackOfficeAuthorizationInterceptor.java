package it.pagopa.selfcare.dashboard.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class BackOfficeAuthorizationInterceptor implements RequestInterceptor {
    @Value("${backoffice.pago-pa.subscriptionKey}")
    private String pagoPAApiSubscriptionKey;

    @Override
    public void apply(RequestTemplate template) {
        template.header("Ocp-Apim-Subscription-Key", pagoPAApiSubscriptionKey);
    }
}
