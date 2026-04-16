package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;

import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

public class RegistryManagerSELCGsp extends RegistryManagerSELC {

    public RegistryManagerSELCGsp(Onboarding onboarding) {
        super(onboarding);
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        return super.customValidation(product)
                .onItem()
                .transformToUni(
                        onboarding -> {
                            if (verifyAdditionalInformation(onboarding)
                            ) {
                                return Uni.createFrom()
                                        .failure(
                                                new InvalidRequestException(
                                                        BaseRegistryManager.ADDITIONAL_INFORMATION_REQUIRED));
                            } else if (verifyAdditionalProperties(onboarding)) {
                                return Uni.createFrom()
                                        .failure(new InvalidRequestException(BaseRegistryManager.OTHER_NOTE_REQUIRED));
                            }
                            return Uni.createFrom().item(onboarding);
                        });
    }

    private static boolean verifyAdditionalProperties(Onboarding onboarding) {
        return PROD_PAGOPA.getValue().equals(onboarding.getProductId())
                && !onboarding.getAdditionalInformations().isIpa()
                && !onboarding.getAdditionalInformations().isBelongRegulatedMarket()
                && !onboarding.getAdditionalInformations().isEstablishedByRegulatoryProvision()
                && !onboarding.getAdditionalInformations().isAgentOfPublicService()
                && Objects.isNull(onboarding.getAdditionalInformations().getOtherNote());
    }

    private static boolean verifyAdditionalInformation(Onboarding onboarding) {
        return PROD_PAGOPA.getValue().equals(onboarding.getProductId())
                && Objects.isNull(onboarding.getAdditionalInformations());
    }

}