package it.pagopa.selfcare.commons.base.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = TestApplication.class
)
@ExtendWith(SystemStubsExtension.class)
@Disabled
class LogUtilsTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @Test
    void confidentialFilterLogging_enabled() {
        // given
        environmentVariables.set("ENABLE_CONFIDENTIAL_FILTER" , "TRUE");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(listAppender);
        rootLogger.setLevel(Level.INFO);
        // when
        log.info(LogUtils.CONFIDENTIAL_MARKER, "confidential");
        // then
        assertEquals(1, listAppender.list.size());
    }

    @Test
    void confidentialFilterLogging_disabled() {
        // given
        environmentVariables.set("ENABLE_CONFIDENTIAL_FILTER" , "FALSE");

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(listAppender);
        rootLogger.setLevel(Level.INFO);
        // when
        log.info(LogUtils.CONFIDENTIAL_MARKER, "confidential");
        // then
        assertEquals(0, listAppender.list.size());
    }
}