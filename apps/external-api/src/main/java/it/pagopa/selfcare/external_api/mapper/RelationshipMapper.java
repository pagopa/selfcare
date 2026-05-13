package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.institution.Onboarding;
import it.pagopa.selfcare.external_api.model.user.ProductInfo;
import it.pagopa.selfcare.external_api.model.user.RelationshipInfo;
import it.pagopa.selfcare.external_api.model.user.RelationshipResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.NONE)
public class RelationshipMapper {

    public static RelationshipResult toRelationshipResult(RelationshipInfo info) {

        RelationshipResult relationshipResult = new RelationshipResult();
        if (info.getOnboardedProduct() != null) {
            relationshipResult.setId(info.getOnboardedProduct().getRelationshipId());
            relationshipResult.setState(info.getOnboardedProduct().getStatus());
            relationshipResult.setRole(info.getOnboardedProduct().getRole());

            ProductInfo productInfo = new ProductInfo();
            productInfo.setId(info.getOnboardedProduct().getProductId());
            productInfo.setCreatedAt(info.getOnboardedProduct().getCreatedAt());
            productInfo.setRole(info.getOnboardedProduct().getProductRole());

            relationshipResult.setProduct(productInfo);
        }
        relationshipResult.setFrom(info.getUserId());
        relationshipResult.setTo(info.getInstitution().getId());

        if (info.getInstitution() != null) {
            relationshipResult.setInstitutionUpdate(InstitutionMapperCustom.toInstitutionUpdateResponse(info.getInstitution()));
        }

        addInstitutionOnboardingData(info, relationshipResult);
        return relationshipResult;
    }

    private static void addInstitutionOnboardingData(RelationshipInfo info, RelationshipResult relationshipResult) {
        if (info.getInstitution().getOnboarding() != null) {
            for (Onboarding onboarding : info.getInstitution().getOnboarding()) {
                if (onboarding.getProductId().equalsIgnoreCase(info.getOnboardedProduct().getProductId())) {
                    relationshipResult.setPricingPlan(onboarding.getPricingPlan());
                    relationshipResult.setBilling(InstitutionMapperCustom.toBillingResponse(onboarding, info.getInstitution()));
                }
            }
        }
    }

    public static List<RelationshipResult> toRelationshipResultList(List<RelationshipInfo> relationshipInfoList) {
        List<RelationshipResult> relationshipResults = new ArrayList<>();
        for (RelationshipInfo info : relationshipInfoList) {
            relationshipResults.add(toRelationshipResult(info));
        }
        return relationshipResults;
    }
}
