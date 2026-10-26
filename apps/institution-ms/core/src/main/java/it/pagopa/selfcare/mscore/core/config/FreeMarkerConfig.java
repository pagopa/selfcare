package it.pagopa.selfcare.mscore.core.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
@EnableAutoConfiguration(exclude = FreeMarkerAutoConfiguration.class)
class FreeMarkerConfig implements AsyncConfigurer {

    @Bean
    public SimpleAsyncUncaughtExceptionHandler simpleAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return simpleAsyncUncaughtExceptionHandler();
    }


    @Bean
    public DelegatingSecurityContextAsyncTaskExecutor taskExecutor(TaskExecutorBuilder taskExecutorBuilder) {
        final ThreadPoolTaskExecutor delegate = taskExecutorBuilder.build();
        delegate.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }

}
