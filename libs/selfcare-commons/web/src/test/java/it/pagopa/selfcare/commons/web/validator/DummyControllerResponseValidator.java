package it.pagopa.selfcare.commons.web.validator;

import jakarta.validation.Validator;

public class DummyControllerResponseValidator extends ControllerResponseValidator {

    public DummyControllerResponseValidator(Validator validator) {
        super(validator);
    }

    @Override
    public void controllersPointcut() {
    }
}
