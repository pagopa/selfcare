package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InstitutionTypeNotAllowedException;
import org.openapi.quarkus.product_json.model.OriginEntry;
import org.openapi.quarkus.product_json.model.ProductResponse;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Checks the institutionType requested against the institutionOrigins configured on the product (product MS).
 * The institutionType must be listed there: a product without institutionOrigins accepts none.
 */
public final class InstitutionTypeValidator {

    public static final String NOT_ALLOWED_INSTITUTION_TYPE =
            "institution with institution type %s is not allowed to onboard product %s";

    private InstitutionTypeValidator() {
    }

    public static Optional<InstitutionTypeNotAllowedException> validate(ProductResponse product,
                                                                        InstitutionType institutionType) {
        if (Objects.isNull(product) || Objects.isNull(institutionType)
                || isAllowedByOrigins(product.getInstitutionOrigins(), institutionType)) {
            return Optional.empty();
        }
        return Optional.of(new InstitutionTypeNotAllowedException(
                String.format(NOT_ALLOWED_INSTITUTION_TYPE, institutionType.name(), product.getProductId())));
    }

    private static boolean isAllowedByOrigins(Collection<OriginEntry> origins, InstitutionType institutionType) {
        return Objects.nonNull(origins) && origins.stream()
                .anyMatch(entry -> Objects.nonNull(entry.getInstitutionType())
                        && institutionType.name().equals(entry.getInstitutionType().name()));
    }
}
