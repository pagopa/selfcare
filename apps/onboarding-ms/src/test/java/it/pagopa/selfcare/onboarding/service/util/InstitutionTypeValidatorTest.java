package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InstitutionTypeNotAllowedException;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.OriginEntry;
import org.openapi.quarkus.product_json.model.ProductResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InstitutionTypeValidatorTest {

    private ProductResponse productWithOrigins(String... types) {
        ProductResponse product = new ProductResponse();
        product.setProductId("prod-pagopa");
        product.setInstitutionOrigins(java.util.Arrays.stream(types)
                .map(t -> new OriginEntry()
                        .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.valueOf(t))
                        .origin(org.openapi.quarkus.product_json.model.Origin.SELC))
                .toList());
        return product;
    }

    @Test
    void validate_institutionTypeNotInProductOrigins_returnsCustomException() {
        Optional<InstitutionTypeNotAllowedException> result =
                InstitutionTypeValidator.validate(productWithOrigins("GPU", "PSP"), InstitutionType.PRV);

        assertTrue(result.isPresent());
        assertEquals("institution with institution type PRV is not allowed to onboard product prod-pagopa",
                result.get().getMessage());
    }

    @Test
    void validate_institutionTypeInProductOrigins_returnsEmpty() {
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins("GPU", "PRV"), InstitutionType.GPU).isEmpty());
    }

    @Test
    void validate_productWithoutOrigins_returnsCustomException() {
        assertTrue(InstitutionTypeValidator.validate(new ProductResponse(), InstitutionType.PRV).isPresent());
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins(), InstitutionType.PRV).isPresent());
    }

    @Test
    void validate_nullInstitutionType_returnsEmpty() {
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins("PA"), null).isEmpty());
    }
}
