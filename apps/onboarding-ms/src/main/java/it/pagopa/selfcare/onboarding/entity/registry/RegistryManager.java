package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.product_json.model.ProductResponse;

public interface RegistryManager<T> {

    T retrieveInstitution();

    // Method used for additional checks
    Uni<Onboarding> customValidation(ProductResponse product);

    // Method used to check correspondence between registry and onboarding data
    Uni<Boolean> isValid();

    Onboarding getOnboarding();

    RegistryManager<T> setResource(T registryResource);

    Uni<Onboarding> validateInstitutionType(ProductResponse product);
}

