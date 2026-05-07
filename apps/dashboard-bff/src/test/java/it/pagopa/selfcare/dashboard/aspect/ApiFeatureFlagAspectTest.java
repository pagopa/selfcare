package it.pagopa.selfcare.dashboard.aspect;

import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiFeatureFlagAspectTest {

    @Test
    public void aroundApiFeatureFlagAnnotatedMethodTestFalse() {
        final Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(false);
        final ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        final ApiFeatureFlag apiFeatureFlag = Mockito.mock(ApiFeatureFlag.class);
        final ApiFeatureFlagAspect apiFeatureFlagAspect = new ApiFeatureFlagAspect(environment);
        assertThrows(ResourceNotFoundException.class, () -> apiFeatureFlagAspect.aroundApiFeatureFlagAnnotatedMethod(pjp, apiFeatureFlag));
    }

    @Test
    public void aroundApiFeatureFlagAnnotatedMethodTestTrue() {
        final Environment environment = Mockito.mock(Environment.class);
        Mockito.when(environment.getProperty(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        final ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        final ApiFeatureFlag apiFeatureFlag = Mockito.mock(ApiFeatureFlag.class);
        final ApiFeatureFlagAspect apiFeatureFlagAspect = new ApiFeatureFlagAspect(environment);
        assertDoesNotThrow(() -> apiFeatureFlagAspect.aroundApiFeatureFlagAnnotatedMethod(pjp, apiFeatureFlag));
    }

}
