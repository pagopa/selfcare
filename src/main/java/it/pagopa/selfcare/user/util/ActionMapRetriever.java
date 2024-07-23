package it.pagopa.selfcare.user.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
@Getter
@Startup
public class ActionMapRetriever {

    private Map<PartyRole, List<String>> userActionsMap;
    private static final String ACTIONS_FILE_PATH = "src/main/resources/role_action_mapping.json";

    public ActionMapRetriever() {
        this.userActionsMap = retrieveActionsMap();
    }

    private Map<PartyRole, List<String>>  retrieveActionsMap() {
        try {
            byte[] jsonFile = Files.readAllBytes(Paths.get(ACTIONS_FILE_PATH));
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("retrieved file with actions map");
            userActionsMap = objectMapper.readValue(jsonFile, new TypeReference<>() {});
            log.info("Action map retrieved successfully");
            return userActionsMap;
        } catch (Exception e) {
            throw new InvalidRequestException("Error during retrieve actions map");
        }
    }
}
