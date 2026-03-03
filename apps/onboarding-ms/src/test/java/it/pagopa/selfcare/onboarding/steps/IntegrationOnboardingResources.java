package it.pagopa.selfcare.onboarding.steps;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ApplicationScoped
public class IntegrationOnboardingResources {
  private final Map<String, String> jsonTemplates = new HashMap<>();

  @PostConstruct
  public void init() {
    loadJsonTemplatesFromDirectory("integration-data");
  }

  private void loadJsonTemplatesFromDirectory(String directoryPath) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      URL resourceDirectory = classLoader.getResource(directoryPath);

      if (resourceDirectory == null) {
        System.err.println("Directory delle risorse non trovata: " + directoryPath);
        return;
      }

      File directory = new File(resourceDirectory.toURI());

      if (directory.isDirectory()) {
        Files.walk(directory.toPath())
            .filter(Files::isRegularFile)
            .forEach(
                path -> {
                  if (path.toString().endsWith(".json")) {
                    File file = path.toFile();
                    String nameFile = file.getName();
                    String templateName = nameFile.replace(".json", "");
                    appendToMap(path, templateName, file);
                  }
                });
      }
    } catch (Exception e) {
      log.error("Errore nel caricamento dei template JSON dalla directory: {}", directoryPath, e);
    }
  }

  private void appendToMap(Path path, String templateName, File file) {
    try {
      String content = Files.readString(path);
      getJsonTemplates().put(templateName, content);
      log.debug("Template name: {}", templateName);
    } catch (IOException e) {
      log.error("Errore nella lettura del template JSON: {}", file.getName(), e);
    }
  }

  public String getJsonTemplate(String templateName) {
    return getJsonTemplates().get(templateName);
  }
}
