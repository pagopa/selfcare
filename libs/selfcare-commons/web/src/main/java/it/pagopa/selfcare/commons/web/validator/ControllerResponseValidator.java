package it.pagopa.selfcare.commons.web.validator;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.*;

@Slf4j
@Aspect
public abstract class ControllerResponseValidator {

    private final Validator validator;


    public ControllerResponseValidator(Validator validator) {
        this.validator = validator;
    }


    public abstract void controllersPointcut();


    @AfterReturning(pointcut = "controllersPointcut()", returning = "result")
    public void validateResponse(JoinPoint joinPoint, Object result) {

        log.trace("validateResponse");
        log.debug("result = {}", result);

        if (result != null) {
            if (Collection.class.isAssignableFrom(result.getClass())) {
                ((Collection<?>) result).forEach(this::validate);
            } else {
                validate(result);
            }
        }
    }

    private void validate(Object result) {
        Set<ConstraintViolation<Object>> validationResults = validator.validate(result);
        if (validationResults.size() > 0) {
            Map<String, List<String>> errorMessage = new HashMap<>();
            validationResults.forEach((error) -> {
                String fieldName = error.getPropertyPath().toString();
                errorMessage.computeIfAbsent(fieldName, s -> new ArrayList<>())
                        .add(error.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName() + " constraint violation");
            });
            throw new RuntimeException(errorMessage.toString());
        }
    }

}
