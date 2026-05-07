package it.pagopa.selfcare.dashboard.config;

import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.net.URL;
import java.util.Properties;

@Slf4j
@Configuration
@PropertySource("classpath:config/core-config.properties")
@EnableAsync
@EnableAutoConfiguration(exclude = FreeMarkerAutoConfiguration.class)
public class CoreConfig implements AsyncConfigurer {

    private final URL rootTemplateUrl;

    @Autowired
    public CoreConfig(@Value("${dashboard.notification.template.default-url}") URL rootTemplateUrl) {
        log.trace("Initializing {}", CoreConfig.class.getSimpleName());
        this.rootTemplateUrl = rootTemplateUrl;
    }

    @Bean
    public freemarker.template.Configuration customFreeMarkerConfiguration() throws TemplateException {
        Properties settings = new Properties();
        settings.put("recognize_standard_file_extensions", "true");
        settings.setProperty("localized_lookup", "false");
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setTemplateLoader(new CloudTemplateLoader(rootTemplateUrl));
        configuration.setSettings(settings);
        return configuration;
    }

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
