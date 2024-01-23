package it.pagopa.selfcare.user.entity.filter;

import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductFilterField.*;

@Builder
public class OnboardedProductFilter {

    private final Object productId;
    private final Object relationshipId;
    private final Object tokenId;
    private final Object status;
    private final Object productRole;
    private final Object role;
    private final Object env;
    private final String createdAt;
    private final String updatedAt;


    @Getter
    @AllArgsConstructor
    public enum OnboardedProductFilterField{
        PRODUCT_ID(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.productId.name()),
        RELATIONSHIP_ID(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.relationshipId.name()),
        TOKEN_ID(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.tokenId.name()),
        STATUS(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.status.name()),
        PRODUCT_ROLE(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.productRole.name()),
        ROLE(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.role.name()),
        ENV(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.env.name()),
        CREATED_AT(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.createdAt.name()),
        UPDATED_AT(UserInstitution.Fields.products.name() + "." + OnboardedProduct.Fields.updatedAt.name());

        private final String description;
    }

    public Map<String, Object> constructMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(PRODUCT_ID.getDescription(), productId);
        map.put(RELATIONSHIP_ID.getDescription(), relationshipId);
        map.put(TOKEN_ID.getDescription(), tokenId);
        map.put(STATUS.getDescription(), status);
        map.put(PRODUCT_ROLE.getDescription(), productRole);
        map.put(ROLE.getDescription(), role);
        map.put(ENV.getDescription(), env);
        map.put(CREATED_AT.getDescription(), createdAt);
        map.put(UPDATED_AT.getDescription(), updatedAt);

        map.values().removeIf(Objects::isNull);

        return map;
    }
}
