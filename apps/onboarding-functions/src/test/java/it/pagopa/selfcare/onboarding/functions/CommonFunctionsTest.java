package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.PurgeInstanceCriteria;
import com.microsoft.durabletask.PurgeResult;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import it.pagopa.selfcare.onboarding.config.PurgeConfig;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommonFunctionsTest {

  @ParameterizedTest
  @CsvSource({"true,false", "false,false", "true,true", "false,true"})
  void purgeClosesClient(boolean completedOnly, boolean timeout) throws Exception {
    PurgeConfig config = mock(PurgeConfig.class);
    when(config.completedFrom()).thenReturn(30L);
    when(config.completedTo()).thenReturn(7L);
    when(config.allFrom()).thenReturn(90L);
    when(config.allTo()).thenReturn(30L);
    CommonFunctions functions = new CommonFunctions(config);
    DurableClientContext durableContext = mock(DurableClientContext.class);
    DurableTaskClient client = mock(DurableTaskClient.class);
    when(durableContext.getClient()).thenReturn(client);
    ExecutionContext context = mock(ExecutionContext.class);
    when(context.getLogger()).thenReturn(mock(Logger.class));
    TimeoutException failure = new TimeoutException("Purge timed out");
    if (timeout) {
      when(client.purgeInstances(any(PurgeInstanceCriteria.class))).thenThrow(failure);
    } else {
      // The SDK exposes no public constructor for this final result type.
      var constructor = PurgeResult.class.getDeclaredConstructor(int.class);
      constructor.setAccessible(true);
      when(client.purgeInstances(any(PurgeInstanceCriteria.class)))
          .thenReturn(constructor.newInstance(1));
    }

    Executable purge = () -> {
      if (completedOnly) {
        functions.purgeInstances("", durableContext, context);
      } else {
        functions.purgeInstancesAll("", durableContext, context);
      }
    };
    if (timeout) {
      assertSame(failure, assertThrows(TimeoutException.class, purge));
    } else {
      assertDoesNotThrow(purge);
    }

    var order = inOrder(client);
    order.verify(client).purgeInstances(any(PurgeInstanceCriteria.class));
    order.verify(client).close();
    verify(client).close();
    verify(durableContext).getClient();
  }
}
