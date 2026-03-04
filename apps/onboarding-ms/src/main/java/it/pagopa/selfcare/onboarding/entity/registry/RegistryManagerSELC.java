package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Objects;

public class RegistryManagerSELC extends BaseRegistryManager<Object> {

    public RegistryManagerSELC(Onboarding onboarding) {
        super(onboarding);
    }

    public Object retrieveInstitution() {
        setDefaultOriginAndOriginId();
        return Uni.createFrom().item(new Object());
    }

    private void setDefaultOriginAndOriginId() {
        onboarding.getInstitution().setOriginId(onboarding.getInstitution().getTaxCode());
        onboarding.getInstitution().setOrigin(Origin.SELC);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        if (isWorkflowTypeAllowed(onboarding.getWorkflowType()) || Objects.nonNull(product.getParentId())) {
            return Uni.createFrom().item(onboarding);
        }

        return Uni.createFrom().failure(new InvalidRequestException("Invalid workflow type for origin SELC"));
    }

    protected boolean isWorkflowTypeAllowed(WorkflowType workflowType) {
        return workflowType == WorkflowType.FOR_APPROVE ||
                workflowType == WorkflowType.IMPORT ||
                workflowType == WorkflowType.FOR_APPROVE_PT ||
                workflowType == WorkflowType.FOR_APPROVE_GPU ||
                workflowType == WorkflowType.CONFIRMATION ||
                workflowType == WorkflowType.CONFIRMATION_AGGREGATOR ||
                workflowType == WorkflowType.INCREMENT_REGISTRATION_AGGREGATOR;
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}