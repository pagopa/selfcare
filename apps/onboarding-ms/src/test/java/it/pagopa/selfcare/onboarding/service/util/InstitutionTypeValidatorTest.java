package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
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
                InstitutionTypeValidator.validate(productWithOrigins("GPU", "PSP"), InstitutionType.PRV, Origin.SELC);

        assertTrue(result.isPresent());
        assertEquals("institution with institution type PRV is not allowed to onboard product prod-pagopa",
                result.get().getMessage());
    }

    @Test
    void validate_institutionTypeInProductOrigins_returnsEmpty() {
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins("GPU", "PRV"), InstitutionType.GPU, Origin.SELC).isEmpty());
    }

    @Test
    void validate_productWithoutOrigins_returnsCustomException() {
        assertTrue(InstitutionTypeValidator.validate(new ProductResponse(), InstitutionType.PRV, Origin.SELC).isPresent());
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins(), InstitutionType.PRV, Origin.SELC).isPresent());
    }

    @Test
    void validate_nullInstitutionType_returnsCustomException() {
        assertTrue(InstitutionTypeValidator.validate(productWithOrigins("PA"), null, Origin.SELC).isPresent());
    }

    private ProductResponse productWithPair(String type, org.openapi.quarkus.product_json.model.Origin origin) {
        ProductResponse product = new ProductResponse();
        product.setProductId("prod-pagopa");
        product.setInstitutionOrigins(new java.util.ArrayList<>(List.of(new OriginEntry()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.valueOf(type))
                .origin(origin))));
        return product;
    }

    @Test
    void validate_sameInstitutionTypeWithDifferentOrigin_returnsCustomException() {
        ProductResponse product = productWithPair("GSP", org.openapi.quarkus.product_json.model.Origin.IPA);

        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, Origin.SELC).isPresent());
        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, Origin.IPA).isEmpty());
    }

    @Test
    void validate_institutionTypeWithTwoOrigins_acceptsBothPairs() {
        ProductResponse product = productWithPair("GSP", org.openapi.quarkus.product_json.model.Origin.IPA);
        product.getInstitutionOrigins().add(new OriginEntry()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.GSP)
                .origin(org.openapi.quarkus.product_json.model.Origin.SELC));

        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, Origin.IPA).isEmpty());
        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, Origin.SELC).isEmpty());
        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, Origin.ANAC).isPresent());
    }

    @Test
    void validate_nullOrigin_returnsCustomException() {
        ProductResponse product = productWithPair("GSP", org.openapi.quarkus.product_json.model.Origin.IPA);

        assertTrue(InstitutionTypeValidator.validate(product, InstitutionType.GSP, null).isPresent());
    }
}
