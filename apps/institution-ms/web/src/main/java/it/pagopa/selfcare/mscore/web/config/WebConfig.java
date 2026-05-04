package it.pagopa.selfcare.mscore.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.web.config.BaseWebConfig;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.web.util.EncryptedTaxCodeParamResolver;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.OffsetDateTime;
import java.util.List;

@Configuration
@Import(BaseWebConfig.class)
public class WebConfig implements BeanPostProcessor, WebMvcConfigurer {

    private final UserRegistryConnector userRegistryConnector;

    public WebConfig(UserRegistryConnector userRegistryConnector) {
        this.userRegistryConnector = userRegistryConnector;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (bean instanceof ObjectMapper) {
            ((ObjectMapper) bean).configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        }
        return bean;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new EncryptedTaxCodeParamResolver(userRegistryConnector));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToOffsetDateTimeConverter());
    }

    public static class StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source);
        }
    }
}
