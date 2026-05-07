package it.pagopa.selfcare.dashboard.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import it.pagopa.selfcare.dashboard.config.CoreConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import java.util.Properties;

@TestConfiguration
@Import(CoreConfig.class)
public class CoreTestConfig {

    @Bean
    Configuration customFreeMarkerConfiguration() throws TemplateException {
        Properties settings = new Properties();
        settings.put("recognize_standard_file_extensions", "true");
        settings.setProperty("localized_lookup", "false");
        freemarker.template.Configuration configuration = new freemarker.template.Configuration(freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setTemplateLoader(new SpringTemplateLoader(new DefaultResourceLoader(), "classpath:templates/"));
        configuration.setSettings(settings);
        return configuration;
    }

}