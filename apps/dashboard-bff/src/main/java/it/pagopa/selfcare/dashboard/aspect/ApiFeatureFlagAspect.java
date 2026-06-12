package it.pagopa.selfcare.dashboard.aspect;

import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ApiFeatureFlagAspect {

    private final Environment environment;

    public ApiFeatureFlagAspect(Environment environment) {
        this.environment = environment;
    }

    @Around("@annotation(apiFeatureFlag)")
    public Object aroundApiFeatureFlagAnnotatedMethod(ProceedingJoinPoint pjp, ApiFeatureFlag apiFeatureFlag) throws Throwable {
        final boolean featureEnabled = environment.getProperty(apiFeatureFlag.value(), Boolean.class, false);
        if (!featureEnabled) {
            throw new ResourceNotFoundException("Not Found");
        }
        return pjp.proceed();
    }

}
