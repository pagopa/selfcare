package it.pagopa.selfcare.onboarding.entity.registry;


import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import java.util.Objects;
import lombok.Data;

@Data
public abstract class BaseRegistryManager<T> implements RegistryManager<T> {

    protected static final String ADDITIONAL_INFORMATION_REQUIRED = "Additional Information is required when institutionType is GSP and productId is pagopa";
    protected static final String OTHER_NOTE_REQUIRED = "Other Note is required when other boolean are false";
    protected static final String BILLING_OR_RECIPIENT_CODE_REQUIRED = "Billing and/or recipient code are required";
    protected static final String ONBOARDING_NOT_ALLOWED_ERROR_MESSAGE_NOT_DELEGABLE = "Institution with external id '%s' is not allowed to onboard '%s' product because it is not delegable";
    protected static final String PARENT_TAX_CODE_IS_INVALID = "The tax code of the parent entity of the request does not match the tax code of the parent entity retrieved by IPA";
    protected static final String TAX_CODE_INVOICING_IS_INVALID = "The tax code invoicing of the request does not match any tax code of institutions' hierarchy";
    protected static final String PNPG_INSTITUTION_REGISTRY_NOT_FOUND = "Institution with taxCode %s is not into registry";
    protected static final String NOT_ALLOWED_PRICING_PLAN = "onboarding pricing plan for io-premium is not allowed";
    protected static final String NOT_ALLOWED_INSTITUTION_TYPE = "institution with institution type %s is not allowed to onboard product %s";
    protected static final int DURATION_TIMEOUT = 5;
    protected static final int MAX_NUMBER_ATTEMPTS = 2;

    protected Onboarding onboarding;
    protected T registryResource;

    protected BaseRegistryManager(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public RegistryManager<T> setResource(T registryResource) {
        this.registryResource = registryResource;
        return this;
    }

    public Uni<Onboarding> validateInstitutionType(Product product) {
        if (Objects.nonNull(product.getInstitutionTypesAllowed()) && !product.getInstitutionTypesAllowed().isEmpty()) {
            return product.getInstitutionTypesAllowed().stream()
                    .anyMatch(type -> type.equals(onboarding.getInstitution().getInstitutionType().name()))
                    ? Uni.createFrom().item(onboarding)
                    : Uni.createFrom().failure(new InvalidRequestException(
                    String.format(NOT_ALLOWED_INSTITUTION_TYPE,
                            onboarding.getInstitution().getInstitutionType().name(),
                            product.getId())
            ));
        }
        return Uni.createFrom().item(onboarding);
    }

}
