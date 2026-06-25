package it.pagopa.selfcare.dashboard.model.delegation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class DelegationTypeValidator implements ConstraintValidator<DelegationTypeSubset, DelegationType> {
    private DelegationType[] subset;

    @Override
    public void initialize(DelegationTypeSubset constraint) {
        this.subset = constraint.anyOf();
    }

    @Override
    public boolean isValid(DelegationType value, ConstraintValidatorContext context) {
        return value == null || Arrays.asList(subset).contains(value);
    }
}