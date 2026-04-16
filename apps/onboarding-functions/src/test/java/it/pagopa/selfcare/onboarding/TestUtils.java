package it.pagopa.selfcare.onboarding;

import com.microsoft.azure.functions.ExecutionContext;

import java.util.logging.Logger;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestUtils {
    public static ExecutionContext getMockedContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        return context;
    }
}
