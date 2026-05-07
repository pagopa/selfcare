package it.pagopa.selfcare.dashboard.config;

import it.pagopa.selfcare.dashboard.config.MdcTaskDecorator;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcTaskDecoratorTest {

    @Test
    void decorate() {
        // given
        MDC.put("key", "value");
        Map<String, String> expecteContextMap = MDC.getCopyOfContextMap();
        MdcTaskDecorator mdcTaskDecorator = new MdcTaskDecorator();
        Runnable runnable = () -> {
            assertEquals(expecteContextMap, MDC.getCopyOfContextMap());
        };
        // when
        Runnable decorated = mdcTaskDecorator.decorate(runnable);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(decorated);
        executorService.execute(() -> {
            assertNull(MDC.getCopyOfContextMap());
        });
        // then
    }
}