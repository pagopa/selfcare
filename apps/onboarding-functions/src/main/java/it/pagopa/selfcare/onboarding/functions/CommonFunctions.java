package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import com.microsoft.durabletask.OrchestrationRuntimeStatus;
import com.microsoft.durabletask.PurgeInstanceCriteria;
import com.microsoft.durabletask.PurgeResult;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import com.microsoft.durabletask.azurefunctions.DurableClientInput;
import it.pagopa.selfcare.onboarding.config.PurgeConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CommonFunctions {

    public static final String FORMAT_LOGGER_ONBOARDING_STRING = "%s: %s";

    private final PurgeConfig purgeConfig;

    public CommonFunctions(PurgeConfig purgeConfig) {
        this.purgeConfig = purgeConfig;
    }

    @FunctionName("PurgeInstancesCompleted")
    public void purgeInstances(
            @TimerTrigger(name = "purgeTimer", schedule = "0 0 11 * * *") String timerInfo,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            ExecutionContext context) throws TimeoutException {
        PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
                .setCreatedTimeFrom(Instant.now().minus(Duration.ofDays(purgeConfig.completedFrom())))
                .setCreatedTimeTo(Instant.now().minus(Duration.ofDays(purgeConfig.completedTo())))
                .setRuntimeStatusList(List.of(OrchestrationRuntimeStatus.COMPLETED));
        PurgeResult result = durableContext.getClient().purgeInstances(criteria);
        context.getLogger().info(String.format("Purged %d instance(s)", result.getDeletedInstanceCount()));
    }

    @FunctionName("PurgeInstancesAll")
    public void purgeInstancesAll(
            @TimerTrigger(name = "purgeTimer", schedule = "0 0 12 * * *") String timerInfo,
            @DurableClientInput(name = "durableContext") DurableClientContext durableContext,
            ExecutionContext context) throws TimeoutException {
        PurgeInstanceCriteria criteria = new PurgeInstanceCriteria()
                .setCreatedTimeFrom(Instant.now().minus(Duration.ofDays(purgeConfig.allFrom())))
                .setCreatedTimeTo(Instant.now().minus(Duration.ofDays(purgeConfig.allTo())));
        PurgeResult result = durableContext.getClient().purgeInstances(criteria);
        context.getLogger().info(String.format("Purged %d instance(s)", result.getDeletedInstanceCount()));
    }
}
