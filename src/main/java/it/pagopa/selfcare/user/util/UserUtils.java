package it.pagopa.selfcare.user.util;

import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.entity.UserInstitution;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    private final ProductService productService;

    @SafeVarargs
    public final Map<String, Object> retrieveMapForFilter(Map<String, Object>... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }

    public void checkProductRole(String productId, PartyRole role, String productRole) {
        if(StringUtils.isNotBlank(productRole) && StringUtils.isNotBlank(productId)) {
            try {
                productService.validateProductRole(productId, productRole, role);
            }catch (IllegalArgumentException e){
                throw new InvalidRequestException(e.getMessage());
            }
        }
    }

    public static boolean checkIfNotFoundException(Throwable throwable) {
        if(throwable instanceof WebClientApplicationException wex) {
            return wex.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND;
        }

        return false;
    }
    /**
     * The filterProduct function takes in a UserInstitution object and an array of states.
     * It then creates a list of OnboardedProductState objects from the array of strings, if the array is not null.
     * If it is null, it sets relationshipStates to be null as well.
     * Then, for each product in userInstitution's products list:
     * if relationshipStates is not null and does not contain that product's status (which should be an OnboardedProductState),
     * remove that product from the list.
     */
    public UserInstitution filterProduct(UserInstitution userInstitution, String[] states) {
        List<OnboardedProductState> relationshipStates = Optional.ofNullable(states)
                .map(this::convertStatesToRelationshipsState)
                .orElse(null);

        userInstitution.getProducts().removeIf(onboardedProduct -> !Objects.isNull(relationshipStates) && !relationshipStates.contains(onboardedProduct.getStatus()));
        return userInstitution;
    }

    public List<OnboardedProductState> convertStatesToRelationshipsState(String[] states) {
        return Arrays.stream(states)
                .map(OnboardedProductState::valueOf)
                .collect(Collectors.toList());
    }
}