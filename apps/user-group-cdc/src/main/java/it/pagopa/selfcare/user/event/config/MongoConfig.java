package it.pagopa.selfcare.user.event.config;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.enterprise.inject.Produces;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import com.mongodb.MongoClientSettings;

@ApplicationScoped
public class MongoConfig {

    @Produces
    public CodecRegistry produceCodecRegistry() {
        CodecRegistry defaultCodecRegistry = MongoClientSettings.getDefaultCodecRegistry();
        CodecRegistry customCodecRegistry = CodecRegistries.fromCodecs(new DateCodec());

        return CodecRegistries.fromRegistries(customCodecRegistry, defaultCodecRegistry);
    }
}
