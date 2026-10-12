package it.pagopa.selfcare.dashboard.config.restclient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import it.pagopa.selfcare.dashboard.client.DocumentContentRestClient;
import it.pagopa.selfcare.dashboard.client.DocumentRestClient;
import it.pagopa.selfcare.dashboard.interceptor.TenantHeaderInterceptor;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class DocumentRestClientConfigTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void documentFeignClientsUseDocumentRestClientConfig() {
        assertArrayEquals(
                new Class<?>[] {DocumentRestClientConfig.class},
                DocumentRestClient.class.getAnnotation(FeignClient.class).configuration());
        assertArrayEquals(
                new Class<?>[] {DocumentRestClientConfig.class},
                DocumentContentRestClient.class.getAnnotation(FeignClient.class).configuration());
    }

    @Test
    void documentRestClientConfigImportsTenantHeaderInterceptor() {
        Import importAnnotation = DocumentRestClientConfig.class.getAnnotation(Import.class);

        assertTrue(Arrays.asList(importAnnotation.value()).contains(TenantHeaderInterceptor.class));
    }

    @Test
    void documentRestClientConfigRegistersAnInterceptorThatForwardsTheTenantHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "AR");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class);
            context.register(DocumentRestClientConfig.class);
            context.refresh();

            assertFalse(context.getBeansOfType(TenantHeaderInterceptor.class).isEmpty());
            RequestTemplate template = new RequestTemplate();
            for (RequestInterceptor interceptor : context.getBeansOfType(RequestInterceptor.class).values()) {
                if (interceptor instanceof TenantHeaderInterceptor) {
                    interceptor.apply(template);
                }
            }

            Collection<String> tenantHeaders = template.headers().get("X-Tenant-Id");
            assertEquals(1, tenantHeaders.size());
            assertEquals("AR", tenantHeaders.iterator().next());
        }
    }
}
