package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.model.user.UserBinding;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.mscore.constant.CustomError.ONBOARDING_INFO_INSTITUTION_NOT_FOUND;
import static it.pagopa.selfcare.mscore.core.util.UtilEnumList.ONBOARDING_INFO_DEFAULT_RELATIONSHIP_STATES;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public class OnboardingInfoUtils {

    public static List<RelationshipState> convertStatesToRelationshipsState(String[] states) {
        return Arrays.stream(states)
                .map(RelationshipState::valueOf)
                .collect(Collectors.toList());
    }

    public static List<UserBinding> getUserInstitutionsWithProductStatusIn(List<UserBinding> userInstitutionToBeFiltered, List<RelationshipState> relationshipStateList) {
        List<UserBinding> result = new ArrayList<>();
        for(UserBinding userBinding: userInstitutionToBeFiltered){
            List<OnboardedProduct> products = userBinding.getProducts().stream().filter(product -> relationshipStateList.contains(product.getStatus())).collect(Collectors.toList());
            if(!products.isEmpty()){
                userBinding.setProducts(products);
                result.add(userBinding);
            }
        }
        if (result.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ONBOARDING_INFO_INSTITUTION_NOT_FOUND.getMessage(), "states : " + StringUtils.join(relationshipStateList, ", ")), ONBOARDING_INFO_INSTITUTION_NOT_FOUND.getCode());
        }
        return result;
    }

    public static Institution findOnboardingLinkedToProductWithStateIn(UserBinding userBinding, Institution onboardedInstitution, List<RelationshipState> relationshipStateList) {
        List<Onboarding> onboardings = new ArrayList<>();
        for(OnboardedProduct product: userBinding.getProducts()){
            onboardings.addAll(onboardedInstitution.getOnboarding().stream().filter(onboarding -> product.getProductId().equalsIgnoreCase(onboarding.getProductId())
                    && relationshipStateList.contains(onboarding.getStatus()))
                    .collect(Collectors.toList()));
        }
        onboardedInstitution.setOnboarding(onboardings);
        return onboardedInstitution;
    }

    public static List<RelationshipState> getRelationShipStateList(String[] states) {
        if (states == null || states.length == 0) {
            return ONBOARDING_INFO_DEFAULT_RELATIONSHIP_STATES;
        } else {
            return convertStatesToRelationshipsState(states);
        }
    }
}
