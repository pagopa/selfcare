package it.pagopa.selfcare.delegation.event.config;

import com.mongodb.MongoClientSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.bson.codecs.configuration.CodecRegistry;

@ApplicationScoped
public class MongoConfig {

    @Produces
    public CodecRegistry produceCodecRegistry() {
        return MongoClientSettings.getDefaultCodecRegistry();
    }
}
