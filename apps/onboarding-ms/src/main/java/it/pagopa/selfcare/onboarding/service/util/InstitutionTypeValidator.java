package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.exception.InstitutionTypeNotAllowedException;
import org.openapi.quarkus.product_json.model.OriginEntry;
import org.openapi.quarkus.product_json.model.ProductResponse;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Checks the (institutionType, origin) pair requested against the institutionOrigins configured on the product
 * (product MS). The same institutionType may be listed with different origins, so the pair must match.
 * Both values are mandatory: a request missing either one cannot be verified and is rejected.
 * A product without institutionOrigins accepts none.
 */
public final class InstitutionTypeValidator {

    public static final String NOT_ALLOWED_INSTITUTION_TYPE =
            "institution with institution type %s and origin %s is not allowed to onboard product %s";

    private InstitutionTypeValidator() {
    }

    public static Optional<InstitutionTypeNotAllowedException> validate(ProductResponse product,
                                                                        InstitutionType institutionType,
                                                                        Origin origin) {
        if (Objects.isNull(product)
                || (Objects.nonNull(institutionType) && Objects.nonNull(origin)
                && isAllowedByOrigins(product.getInstitutionOrigins(), institutionType, origin))) {
            return Optional.empty();
        }
        return Optional.of(new InstitutionTypeNotAllowedException(
                String.format(NOT_ALLOWED_INSTITUTION_TYPE, institutionType, origin, product.getProductId())));
    }

    private static boolean isAllowedByOrigins(Collection<OriginEntry> origins, InstitutionType institutionType,
                                              Origin origin) {
        return Objects.nonNull(origins) && origins.stream()
                .anyMatch(entry -> Objects.nonNull(entry.getInstitutionType())
                        && institutionType.name().equals(entry.getInstitutionType().name())
                        && Objects.nonNull(entry.getOrigin())
                        && origin.name().equals(entry.getOrigin().name()));
    }
}
