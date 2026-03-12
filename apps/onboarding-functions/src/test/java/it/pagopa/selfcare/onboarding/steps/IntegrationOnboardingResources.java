package it.pagopa.selfcare.onboarding.steps;

import static it.pagopa.selfcare.onboarding.utils.Utils.readOnboardingValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@ApplicationScoped
public class IntegrationOnboardingResources {
  private final Map<String, Onboarding> onboardingTemplateMap = new HashMap<>();
  private final Map<String, Institution> institutionTemplateMap = new HashMap<>();

  @PostConstruct
  public void init() {
    loadOnboardingsJsonTemplatesFromDirectory("integration_data/onboarding");
    loadInstitutionJsonTemplatesFromDirectory("integration_data/institution");
  }

  private void loadOnboardingsJsonTemplatesFromDirectory(String directoryPath) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      URL resourceDirectory = classLoader.getResource(directoryPath);

      if (resourceDirectory == null) {
        log.error("Directory delle risorse non trovata: {}", directoryPath);
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
                    appendToOnboardingMap(path, templateName, file);
                  }
                });
      }
    } catch (Exception e) {
      log.error(
          "Errore nel caricamento dei template Onboarding JSON dalla directory: {}",
          directoryPath,
          e);
    }
  }

  private void appendToOnboardingMap(Path path, String templateName, File file) {
    try {

      String content = Files.readString(path);

      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());

      Onboarding onboarding = readOnboardingValue(mapper, content);

      getOnboardingTemplateMap().put(onboarding.getId(), onboarding);
      log.debug("Template name: {} - id: {}", templateName, onboarding.getId());
    } catch (IOException e) {
      log.error("Errore nella lettura del template JSON: {}", file.getName(), e);
    }
  }

  private void loadInstitutionJsonTemplatesFromDirectory(String directoryPath) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      URL resourceDirectory = classLoader.getResource(directoryPath);

      if (resourceDirectory == null) {
        log.error("Directory delle risorse non trovata: {}", directoryPath);
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
                    log.info(file.getAbsolutePath());
                    try {
                      ObjectMapper mapper = new ObjectMapper();
                      List<Institution> institutions = mapper.readValue(file, new TypeReference<List<Institution>>() {});
                      for (Institution current : institutions) {
                        appendToInstitutionMap(current);
                      }

                    } catch (IOException e) {
                      e.printStackTrace();
                      log.error("Errore nella lettura del file JSON: " + file.getName());
                    }
                  }
                });
      }
    } catch (Exception e) {
      log.error(
          "Errore nel caricamento dei template Institution JSON dalla directory: {}",
          directoryPath,
          e);
    }
  }

  private void appendToInstitutionMap(Institution institution) {
    getInstitutionTemplateMap().put(institution.getTaxCode(), institution);
    log.debug("Institution taxCode: {}", institution.getTaxCode());
  }

  public Onboarding getOnboardingJsonTemplate(String templateName) {
    return getOnboardingTemplateMap().get(templateName);
  }

  public Institution getInstitutionJsonTemplate(String templateName) {
    return getInstitutionTemplateMap().get(templateName);
  }
}
