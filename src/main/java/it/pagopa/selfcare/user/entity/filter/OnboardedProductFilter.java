package it.pagopa.selfcare.user.entity.filter;

import it.pagopa.selfcare.user.entity.OnboardedProduct;
import it.pagopa.selfcare.user.entity.UserInstitution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

import static it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter.OnboardedProductEnum.*;

@Builder
public class OnboardedProductFilter {

    private final Object productId;
    private final Object tokenId;
    private final Object status;
    private final Object productRole;
    private final Object role;
    private final Object env;
    private final String createdAt;
    private final String updatedAt;


    @Getter
    @AllArgsConstructor
    public enum OnboardedProductEnum {
        PRODUCT_ID(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.productId.name()),
        TOKEN_ID(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.tokenId.name()),
        STATUS(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.status.name()),
        PRODUCT_ROLE(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.productRole.name()),
        ROLE(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.role.name()),
        ENV(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.env.name()),
        CREATED_AT(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.createdAt.name()),
        UPDATED_AT(UserInstitution.Fields.products.name(), OnboardedProduct.Fields.updatedAt.name());

        private final String parent;
        private final String child;

        public static Optional<String> retrieveParent(String child){
            return Arrays.stream(values())
                    .filter(onboardedProductEnum -> onboardedProductEnum.getChild().equalsIgnoreCase(child))
                    .findFirst()
                    .map(OnboardedProductEnum::getParent);
        }
    }

    public Map<String, Object> constructMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(PRODUCT_ID.getChild(), productId);
        map.put(TOKEN_ID.getChild(), tokenId);
        map.put(STATUS.getChild(), status);
        map.put(PRODUCT_ROLE.getChild(), productRole);
        map.put(ROLE.getChild(), role);
        map.put(ENV.getChild(), env);
        map.put(CREATED_AT.getChild(), createdAt);
        map.put(UPDATED_AT.getChild(), updatedAt);

        map.entrySet().removeIf(e -> Objects.isNull(e.getValue()) ||
                (e.getValue() instanceof Collection && ((Collection) e.getValue()).isEmpty()));

        return map;
    }
}
