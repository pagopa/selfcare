package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.constants.CustomError.DEFAULT_ERROR;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils;
import org.openapi.quarkus.product_json.model.ProductResponse;

public class RegistryManagerPT extends RegistryManagerSELC {

    public RegistryManagerPT(Onboarding onboarding) {
        super(onboarding);
    }

    @Override
    public Uni<Onboarding> customValidation(ProductResponse product) {
        if (!ProductConfigUtils.delegable(product)) {
            return Uni.createFrom().failure(new OnboardingNotAllowedException(String.format(BaseRegistryManager.ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE,
                    onboarding.getInstitution().getTaxCode(),
                    onboarding.getProductId()), DEFAULT_ERROR.getCode()));
        }

        return super.customValidation(product);
    }
}
