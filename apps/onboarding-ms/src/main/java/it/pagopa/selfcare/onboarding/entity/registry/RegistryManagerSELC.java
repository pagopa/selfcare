package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.product_json.model.ProductResponse;

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

    /** Admission (institution type, origin, workflow) is decided by product-ms before the registry step. */
    @Override
    public Uni<Onboarding> customValidation(ProductResponse product) {
        return Uni.createFrom().item(onboarding);
    }

    @Override
    public Uni<Boolean> isValid() {
        return Uni.createFrom().item(true);
    }

}