package it.pagopa.selfcare.onboarding.connector.rest.config;

import feign.form.ContentType;
import feign.form.MultipartFormContentProcessor;
import feign.form.spring.SpringFormEncoder;
import feign.form.spring.SpringManyMultipartFilesWriter;
import feign.form.spring.SpringSingleMultipartFileWriter;
import it.pagopa.selfcare.commons.connector.rest.config.RestClientBaseConfig;
import org.springframework.cloud.openfeign.support.JsonFormWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import feign.codec.Encoder;

@Configuration
@Import({RestClientBaseConfig.class, JsonFormWriter.class})
public class MultipartFeignConfig {

    @Bean
    @Primary
    Encoder feignEncoder(JsonFormWriter jsonFormWriter) {
        return new SpringFormEncoder() {{
            var processor = (MultipartFormContentProcessor) getContentProcessor(ContentType.MULTIPART);
            processor.addFirstWriter(jsonFormWriter);
            processor.addFirstWriter(new SpringSingleMultipartFileWriter());
            processor.addFirstWriter(new SpringManyMultipartFilesWriter());
        }};
    }
}

