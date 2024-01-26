package it.pagopa.selfcare.user.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.reactive.client.api.WebClientApplicationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    private final ProductService productService;


    public void checkProductRole(String productId, PartyRole role, String productRole) {
        if(StringUtils.isNotBlank(productRole) && StringUtils.isNotBlank(productId)) {
            try {
                productService.validateProductRole(productId, productRole, role);
            }catch (IllegalArgumentException e){
                throw new InvalidRequestException(e.getMessage());
            }
        }
    }

    @SafeVarargs
    public final Map<String, Object> retrieveMapForFilter(Map<String, Object>... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }


    public static boolean checkIfNotFoundException(Throwable throwable) {
        if(throwable instanceof WebClientApplicationException wex) {
            return wex.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
        }

        return false;
    }
}