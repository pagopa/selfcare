package it.pagopa.selfcare.onboarding.service.util;

import static it.pagopa.selfcare.onboarding.common.Origin.IPA;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Objects;
import java.util.Set;

/**
 * Determina il {@link WorkflowType} da associare all'onboarding
 * in base a tipo istituzione, prodotto e flag aggregatore.
 */
@ApplicationScoped
public class WorkflowTypeResolver {

    @Inject
    ProductService productService;

    /**
     * Identifica il workflow corretto per l'onboarding.
     *
     * @param onboarding richiesta di onboarding
     * @return WorkflowType appropriato
     */
    public WorkflowType resolve(Onboarding onboarding) {
        InstitutionType institutionType = onboarding.getInstitution().getInstitutionType();
        Product product = productService.getProductIsValid(onboarding.getProductId());

        if (InstitutionType.PT.equals(institutionType)) {
            return WorkflowType.FOR_APPROVE_PT;
        }

        if (Boolean.TRUE.equals(onboarding.getIsAggregator())) {
            return WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR;
        }

        if (InstitutionType.PA.equals(institutionType)
                || isGspOrScecOnIpa(institutionType, onboarding.getInstitution().getOrigin().getValue())
                || InstitutionType.SA.equals(institutionType)
                || InstitutionType.AS.equals(institutionType)
                || Objects.nonNull(product.getParentId())
                || InstitutionType.PRV_PF.equals(institutionType)
                || (InstitutionType.PRV.equals(institutionType)
                        && !PROD_PAGOPA.getValue().equals(onboarding.getProductId()))) {
            return WorkflowType.CONTRACT_REGISTRATION;
        }

        if (InstitutionType.PG.equals(institutionType)) {
            return WorkflowType.CONFIRMATION;
        }

        if (InstitutionType.GPU.equals(institutionType)) {
            return WorkflowType.FOR_APPROVE_GPU;
        }

        return WorkflowType.FOR_APPROVE;
    }

    private boolean isGspOrScecOnIpa(InstitutionType institutionType, String origin) {
        return Objects.nonNull(institutionType)
                && Set.of(InstitutionType.GSP, InstitutionType.SCEC).contains(institutionType)
                && IPA.getValue().equals(origin);
    }
}

