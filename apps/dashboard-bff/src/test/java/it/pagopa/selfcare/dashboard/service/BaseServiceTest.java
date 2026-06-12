package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class BaseServiceTest {

    protected ObjectMapper objectMapper;

    public BaseServiceTest() {
    }

    public void setUp() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper((new ObjectMapper()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).registerModule(new JavaTimeModule()));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
}
