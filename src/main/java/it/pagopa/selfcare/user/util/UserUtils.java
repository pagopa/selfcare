package it.pagopa.selfcare.user.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;

import static it.pagopa.selfcare.user.constant.CustomError.*;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    private final ProductService productService;

    /**
     * The checkRoles function is used to check if the product role provided by the user
     * matches one of the roles defined in a product. If it does not, an exception is thrown.
     */
    public Uni<Boolean> checkRoles(String productId, PartyRole role, String productRole) {
        if (StringUtils.isNotBlank(productRole) && StringUtils.isNotBlank(productId)) {
            if (role == null) {
                return Uni.createFrom().failure(new InvalidRequestException(ROLE_IS_NULL.getMessage(), ROLE_IS_NULL.getCode()));
            }
            return Uni.createFrom().item(productService.getProduct(productId))
                    .log()
                    .map(product -> product.getRoleMappings().get(role))
                    .map(productRoleInfo -> {
                        if (productRoleInfo == null) {
                            throw new InvalidRequestException(ROLE_NOT_FOUND.getMessage(), ROLE_NOT_FOUND.getCode());
                        }
                        return productRoleInfo.getRoles().stream().anyMatch(prodRole -> prodRole.getCode().equals(productRole));
                    })
                    .map(aBoolean -> {
                        if(Boolean.FALSE.equals(aBoolean)){
                            throw new InvalidRequestException(PRODUCT_ROLE_NOT_FOUND.getMessage());
                        }
                        return aBoolean;
                    });
        }
        return Uni.createFrom().item(true);
    }
}