package it.pagopa.selfcare.onboarding.steps;

import it.pagopa.selfcare.onboarding.utils.JwtData;
import it.pagopa.selfcare.onboarding.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.buildJwtHeader;
import static it.pagopa.selfcare.onboarding.steps.IntegrationFunctionProfile.buildJwtPayload;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@Slf4j
class TokenGeneratorTest {

    @Test
    void generateToken(){
        // given

        // when
        String token = JwtUtils.generateToken(
                JwtData.builder()
                        .username("f.rossi")
                        .password("test")
                        .jwtHeader(buildJwtHeader())
                        .jwtPayload(buildJwtPayload())
                        .build());

        // then
        Assertions.assertNotNull(token);
        assertFalse(token.isEmpty());
        assertFalse(token.isBlank());
        log.info(token);
    }
}
