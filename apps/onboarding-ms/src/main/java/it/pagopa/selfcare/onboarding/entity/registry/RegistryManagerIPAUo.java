package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType.UO;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DENIED_NO_ASSOCIATION;
import static it.pagopa.selfcare.onboarding.constants.CustomError.DENIED_NO_BILLING;
import static it.pagopa.selfcare.onboarding.constants.CustomError.UO_NOT_FOUND;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryIPA;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Objects;
import org.openapi.quarkus.party_registry_proxy_json.api.AooApi;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

public class RegistryManagerIPAUo extends ClientRegistryIPA {

    private static final List<String> ALLOWED_PRICING_PLANS = List.of("C0");

    public RegistryManagerIPAUo(Onboarding onboarding, UoApi uoApi, AooApi aooApi) {
        super(onboarding, uoApi, aooApi);
    }

    public RegistryManagerIPAUo(Onboarding onboarding, UoApi uoApi) {
        super(onboarding, uoApi);
    }

    public RegistryManagerIPAUo(Onboarding onboarding, UoApi uoApi, InstitutionApi institutionApi, GeographicTaxonomiesApi geographicTaxonomiesApi) {
        super(onboarding, uoApi, institutionApi, geographicTaxonomiesApi);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return checkRecipientCode().onItem().transformToUni(unused -> {
            if (!PROD_INTEROP.getValue().equals(onboarding.getProductId()) && !PROD_IO.getValue().equals(onboarding.getProductId())
                    && !onboarding.getInstitution().isImported() && (Objects.isNull(onboarding.getBilling()) || Objects.isNull(onboarding.getBilling().getRecipientCode()))) {
                return Uni.createFrom().failure(new InvalidRequestException(BILLING_OR_RECIPIENT_CODE_REQUIRED));
            } else if (PROD_IO_PREMIUM.getValue().equals(onboarding.getProductId()) &&
                    ALLOWED_PRICING_PLANS.stream().noneMatch(
                            pricingPlan -> pricingPlan.equals(onboarding.getPricingPlan()))) {
                return Uni.createFrom().failure(new InvalidRequestException(BaseRegistryManager.NOT_ALLOWED_PRICING_PLAN));
            }
            return Uni.createFrom().item(onboarding);
        }).onItem().transformToUni(unused -> billingChecks());
    }

    @Override
    public Uni<Boolean> isValid() {
        if (!originIPA(onboarding, registryResource.getUoResource())) {
            return Uni.createFrom().failure(new InvalidRequestException(
                    String.format("Field digitalAddress or description are not valid for institution with taxCode=%s and subunitCode=%s",
                            onboarding.getInstitution().getTaxCode(),
                            onboarding.getInstitution().getSubunitCode())
            ));
        }
        return Uni.createFrom().item(true);
    }

    private boolean originIPA(Onboarding onboarding, UOResource uoResource) {
        return onboarding.getInstitution().getDigitalAddress().equalsIgnoreCase(uoResource.getMail1()) &&
                onboarding.getInstitution().getDescription().equalsIgnoreCase(uoResource.getDescrizioneUo());
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return super.uoClient.findByUnicodeUsingGET1(recipientCode, null)
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format(UO_NOT_FOUND.getMessage(),
                                recipientCode
                        )))
                        : Uni.createFrom().failure(ex));
    }

    protected Uni<Void> checkRecipientCode() {
        if (isInvoiceablePA(onboarding) && !onboarding.getInstitution().isImported()) {
            final String recipientCode = onboarding.getBilling().getRecipientCode();
            return getUoFromRecipientCode(recipientCode)
                    .onItem().transformToUni(this::validateRecipientCode)
                    .onItem().transformToUni(customError -> {
                        if (Objects.nonNull(customError)) {
                            return Uni.createFrom().failure(new InvalidRequestException(customError.getMessage()));
                        }
                        return Uni.createFrom().nullItem();
                    });
        }
        return Uni.createFrom().nullItem();
    }

    protected Uni<CustomError> validateRecipientCode(UOResource uoResource) {
        if (Objects.nonNull(originIdEC) && !originIdEC.equals(uoResource.getCodiceIpa())) {
            return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
        }
        if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
            return Uni.createFrom().item(DENIED_NO_BILLING);
        }
        return Uni.createFrom().nullItem();
    }

    protected boolean isInvoiceablePA(Onboarding onboarding) {
        return InstitutionType.PA.equals(onboarding.getInstitution().getInstitutionType())
                && Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getBilling().getRecipientCode());
    }

    private boolean hasSfe(Onboarding onboarding) {
        return Objects.nonNull(onboarding.getBilling())
                && Objects.nonNull(onboarding.getInstitution().getSubunitCode())
                //Remove this UO condition moving  billing checks into isValid method
                && UO.equals(onboarding.getInstitution().getSubunitType())
                && Objects.nonNull(onboarding.getBilling().getTaxCodeInvoicing())
                && Objects.nonNull(onboarding.getInstitution().getTaxCode());
    }

    private Uni<Void> checkParentTaxCode() {
        /* if parent tax code is different from child tax code, throw an exception */
        if (!onboarding.getInstitution().getTaxCode().equals(resourceTaxCode)) {
            return Uni.createFrom().failure(new InvalidRequestException(PARENT_TAX_CODE_IS_INVALID));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Onboarding> checkTaxCodeInvoicing(Onboarding onboarding) {
        /* if tax code invoicing is not into hierarchy, throw an exception */
        return super.uoClient.findAllUsingGET1(null, null, onboarding.getBilling().getTaxCodeInvoicing())
                .flatMap(uosResource -> {
                    /* if parent tax code is not into hierarchy, throw an exception */
                    if (Objects.nonNull(uosResource) && Objects.nonNull(uosResource.getItems())
                            && uosResource.getItems().stream().anyMatch(uoResource -> !uoResource.getCodiceFiscaleEnte().equals(onboarding.getInstitution().getTaxCode()))) {
                        return Uni.createFrom().failure(new InvalidRequestException(TAX_CODE_INVOICING_IS_INVALID));
                    }
                    return Uni.createFrom().item(onboarding);
                });
    }

    private Uni<Onboarding> billingChecks() {
        if (hasSfe(onboarding)) {
            return checkParentTaxCode()
                    .onItem().transformToUni(ignored -> checkTaxCodeInvoicing(onboarding));
        }
        return Uni.createFrom().item(onboarding);
    }
}
