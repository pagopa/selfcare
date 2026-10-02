package it.pagopa.selfcare.document.config;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Slf4j
@Data
public class DocumentMsConfig {

    public static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";
    
    @ConfigProperty(name = "document-ms.blob-storage.path-contracts")
    String contractPath;

    @ConfigProperty(name = "document-ms.blob-storage.path-aggregates")
    String aggregatesPath;

    @ConfigProperty(name = "document-ms.blob-storage.path-deleted")
    String deletePath;

}
