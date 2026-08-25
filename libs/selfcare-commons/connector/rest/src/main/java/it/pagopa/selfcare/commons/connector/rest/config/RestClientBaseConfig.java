package it.pagopa.selfcare.commons.connector.rest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import it.pagopa.selfcare.commons.connector.rest.interceptor.QueryParamsPlusEncoderInterceptor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@PropertySource("classpath:config/default-rest-client.properties")
public class RestClientBaseConfig {

    @Autowired
    private ObjectMapper objectMapper;


    @Bean
    public RequestInterceptor queryParamsPlusEncoderInterceptor() {
        return new QueryParamsPlusEncoderInterceptor();
    }


    private ResponseEntityDecoder getFeignDecoder(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);

        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }


    @Bean
    @ConditionalOnMissingClass("org.springframework.data.domain.Page")
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return getFeignDecoder(objectMapper);
    }


    @Bean
    @ConditionalOnClass(name = "org.springframework.data.domain.Page")
    public Decoder feignPageDecoder(ObjectMapper objectMapper) {
        return getFeignDecoder(objectMapper.registerModule(new PageJacksonModule()));
    }


    private SpringEncoder getSpringEncoder(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(jacksonConverter);

        return new SpringEncoder(objectFactory);
    }


    @Bean
    @ConditionalOnMissingClass("org.springframework.data.domain.Pageable")
    public Encoder feignEncoder(ObjectMapper objectMapper) {
        return getSpringEncoder(objectMapper);
    }


    @Bean
    @ConditionalOnClass(name = "org.springframework.data.domain.Pageable")
    public Encoder feignEncoderPageable(ObjectMapper objectMapper) {
        return new PageableSpringEncoder(getSpringEncoder(objectMapper));
    }

}
