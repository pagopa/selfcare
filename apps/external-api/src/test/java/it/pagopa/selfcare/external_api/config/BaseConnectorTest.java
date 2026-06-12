package it.pagopa.selfcare.external_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class BaseConnectorTest {
    protected ObjectMapper objectMapper;

    public BaseConnectorTest() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new
                MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule()));
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
}
