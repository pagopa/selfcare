package it.pagopa.selfcare.party.registry_proxy.connector.lucene.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.nio.file.Path;

@Slf4j
@Configuration
@PropertySource("classpath:config/lucene-config.properties")
class FileSystemIndexConfig {

    public FileSystemIndexConfig() {
        log.trace("Initializing {}", FileSystemIndexConfig.class.getSimpleName());
    }

    @SneakyThrows
    @Bean
    public Directory institutionsDirectory(@Value("${lucene.index.institutions.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }

    @SneakyThrows
    @Bean
    public Directory categoriesDirectory(@Value("${lucene.index.categories.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }

    @SneakyThrows
    @Bean
    public Directory aoosDirectory(@Value("${lucene.index.aoos.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }

    @SneakyThrows
    @Bean
    public Directory uosDirectory(@Value("${lucene.index.uos.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }

    @SneakyThrows
    @Bean
    public Directory anacDirectory(@Value("${lucene.index.anac.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }

    @SneakyThrows
    @Bean
    public Directory ivassDirectory(@Value("${lucene.index.ivass.folder}") String indexFolder) {
        return FSDirectory.open(Path.of(indexFolder));
    }
}
