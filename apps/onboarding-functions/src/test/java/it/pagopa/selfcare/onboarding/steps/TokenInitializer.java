package it.pagopa.selfcare.onboarding.steps;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import it.pagopa.selfcare.onboarding.utils.JwtData;
import it.pagopa.selfcare.onboarding.utils.JwtUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.buildJwtHeader;
import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.buildJwtPayload;

@ApplicationScoped
@Slf4j
public class TokenInitializer {

    void onStart(@Observes StartupEvent ev) {
        String activeProfile = ProfileManager.getActiveProfile();

        if ("integration-function".equalsIgnoreCase(activeProfile)) {
            System.setProperty("JWT_BEARER_TOKEN", Objects.requireNonNull(
                    JwtUtils.generateToken(
                            JwtData.builder()
                                    .username("f.rossi")
                                    .password("test")
                                    .jwtHeader(buildJwtHeader())
                                    .jwtPayload(buildJwtPayload())
                                    .build())));

            log.debug("Actived profile:" + activeProfile);
        }
    }
}
