package it.pagopa.selfcare.logavailability;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import it.pagopa.selfcare.logavailability.service.AvailabilityRunException;
import it.pagopa.selfcare.logavailability.service.AvailabilityRunService;
import it.pagopa.selfcare.logavailability.service.RunOptions;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@QuarkusMain
public class Application implements QuarkusApplication {

    private static final Logger LOGGER = Logger.getLogger(Application.class);
    private static final ZoneId ITALY_ZONE = ZoneId.of("Europe/Rome");

    @Inject
    AvailabilityRunService availabilityRunService;

    @ConfigProperty(name = "availability.environment")
    String environment;

    @ConfigProperty(name = "availability.source-retention-days", defaultValue = "90")
    int sourceRetentionDays;

    @Override
    public int run(String... args) {
        RunOptions options;
        try {
            options = RunOptions.parse(args, Instant.now(), ITALY_ZONE, sourceRetentionDays);
        } catch (IllegalArgumentException exception) {
            LOGGER.errorf("Availability job rejected its arguments (%s)", exception.getClass().getSimpleName());
            return 2;
        }

        if (!options.shouldRun()) {
            LOGGER.info("Skipping non-03:00 Europe/Rome scheduled trigger");
            return 0;
        }

        boolean backfill = options.referenceDates().size() > 1;
        LOGGER.infof("Starting availability %s for environment %s (%d date(s))",
                backfill ? "backfill" : "run", environment, options.referenceDates().size());
        for (LocalDate referenceDate : options.referenceDates()) {
            try {
                LOGGER.infof("Computing availability for environment %s and reference date %s",
                        environment, referenceDate);
                availabilityRunService.run(environment, referenceDate);
                LOGGER.infof("Completed availability run for environment %s and reference date %s",
                        environment, referenceDate);
            } catch (AvailabilityRunException exception) {
                LOGGER.errorf("Availability run failed for environment %s, reference date %s, stage %s (%s)",
                        environment, referenceDate, exception.stage(),
                        exception.getCause().getClass().getSimpleName());
                return 1;
            } catch (RuntimeException exception) {
                LOGGER.errorf("Availability run failed for environment %s, reference date %s (%s)",
                        environment, referenceDate, exception.getClass().getSimpleName());
                return 1;
            }
        }
        return 0;
    }
}
