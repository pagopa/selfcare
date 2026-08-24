package it.pagopa.selfcare.commons.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class TargetEnvironmentTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void getCurrent_notSet() {
        // when
        TargetEnvironment current = TargetEnvironment.getCurrent();
        // then
        Assertions.assertNull(current);
    }

    @ParameterizedTest
    @EnumSource(TargetEnvironment.class)
    void getCurrent(TargetEnvironment targetEnvironment) {
        // given
        environmentVariables.set("ENV_TARGET", targetEnvironment);
        // when
        TargetEnvironment current = TargetEnvironment.getCurrent();
        // then
        Assertions.assertEquals(targetEnvironment, current);
    }
}