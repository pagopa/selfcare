package it.pagopa.selfcare.dashboard.validator;

import it.pagopa.selfcare.commons.web.validator.ControllerResponseValidator;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.Validator;

@Aspect
@Component
public class DashboardControllerResponseValidator extends ControllerResponseValidator {

    @Autowired
    public DashboardControllerResponseValidator(Validator validator) {
        super(validator);
    }

    @Override
    @Pointcut("execution(* it.pagopa.selfcare.dashboard.controller.*.*(..))")
    public void controllersPointcut() {
        // Do nothing because is a pointcut
    }

}