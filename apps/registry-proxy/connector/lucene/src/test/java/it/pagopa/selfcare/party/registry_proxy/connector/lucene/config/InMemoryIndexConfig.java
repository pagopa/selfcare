package it.pagopa.selfcare.party.registry_proxy.connector.lucene.config;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class InMemoryIndexConfig {

    @Bean
    public Directory institutionsDirectory() {
        return new RAMDirectory();
    }

    @Bean
    public Directory categoriesDirectory() {
        return new RAMDirectory();
    }

    @Bean
    public Directory aoosDirectory() {
        return new RAMDirectory();
    }

    @Bean
    public Directory uosDirectory() {
        return new RAMDirectory();
    }

    @Bean
    public Directory anacDirectory() { return new RAMDirectory(); }

    @Bean
    public Directory ivassDirectory() { return new RAMDirectory(); }

}
