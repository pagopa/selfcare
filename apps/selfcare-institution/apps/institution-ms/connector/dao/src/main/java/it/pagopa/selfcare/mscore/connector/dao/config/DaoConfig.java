package it.pagopa.selfcare.mscore.connector.dao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
@PropertySource("classpath:config/dao-config.properties")
class DaoConfig{

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new OffsetDateTimeToStringConverter(),
                new StringToOffsetDateTimeConverter()
        ));
    }

}
