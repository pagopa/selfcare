package it.pagopa.selfcare.user.conf;

import freemarker.cache.TemplateLoader;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


@Slf4j
@ApplicationScoped
public class CloudTemplateLoader implements TemplateLoader {

    private final AzureBlobClient azureBlobClient;

    public CloudTemplateLoader(AzureBlobClient azureBlobClient) {
        this.azureBlobClient = azureBlobClient;
    }

    @ConfigProperty(name = "user-ms.blob-storage.filepath-templates")
    private String filePath;

    @Override
    public Object findTemplateSource(String name){
        name = name.replace("_en_US", "");
        return name;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return this.getLastModified((String) templateSource).toEpochSecond(ZoneOffset.UTC);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if (templateSource == null)
            throw new IOException("Template not found");

        String template = this.fetchTemplate((String) templateSource);
        return new StringReader(template);
    }

    @Override
    public void closeTemplateSource(Object o) {
        // Do nothing
    }

    public String fetchTemplate(String templateName) {
        String path = this.filePath + templateName;
        return azureBlobClient.getFileAsText(path);
    }

    public LocalDateTime getLastModified(String templateName) {
        String path = this.filePath + templateName;
        return azureBlobClient.getProperties(path).getLastModified().toLocalDateTime();
    }
}
