package it.pagopa.selfcare.dashboard.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MdcTaskDecorator implements TaskDecorator {

    public MdcTaskDecorator() {
        log.trace("Initializing {}", MdcTaskDecorator.class.getSimpleName());
    }


    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                MDC.setContextMap(contextMap);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

}
