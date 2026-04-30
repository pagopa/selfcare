package it.pagopa.selfcare.user_group.validator;

import it.pagopa.selfcare.commons.web.validator.ControllerResponseValidator;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.Validator;

@Aspect
@Component
public class UserGroupControllerResponseValidator extends ControllerResponseValidator {

    @Autowired
    public UserGroupControllerResponseValidator(Validator validator) {
        super(validator);
    }

    @Override
    @Pointcut("execution(* it.pagopa.selfcare.user_group.web.controller.*.*(..))")
    public void controllersPointcut() {
        // Do nothing because is a pointcut
    }

}
