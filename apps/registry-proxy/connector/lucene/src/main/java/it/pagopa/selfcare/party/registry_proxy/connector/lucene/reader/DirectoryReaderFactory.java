package it.pagopa.selfcare.party.registry_proxy.connector.lucene.reader;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Slf4j
public class DirectoryReaderFactory {

    private final Directory directory;
    private DirectoryReader currentReader;


    @Autowired
    public DirectoryReaderFactory(Directory directory) {
        log.trace("Initializing {}", DirectoryReaderFactory.class.getSimpleName());
        this.directory = directory;
    }


    @SneakyThrows
    public DirectoryReader create() {
        log.trace("create start");
        if (currentReader == null) {
            currentReader = DirectoryReader.open(directory);
        } else {
            currentReader = Optional.ofNullable(DirectoryReader.openIfChanged(currentReader))
                    .orElse(currentReader);
        }
        //log.debug("create result = {}", currentReader);
        log.trace("create end");
        return currentReader;
    }

}
