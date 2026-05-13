package it.pagopa.selfcare.external_api.model.product;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import lombok.Data;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

@Data
public class Product {

    private String id;
    private String logo;
    private String logoBgColor;
    private String depictImageUrl;
    private String title;
    private String description;
    private String urlPublic;
    private String urlBO;
    private OffsetDateTime activatedAt;
    private boolean authorized;
    private String userRole;
    private ProductOnboardingStatus productOnboardingStatus;
    private ProductStatus status;
    private String identityTokenAudience;
    private EnumMap<PartyRole, ProductRoleInfo> roleMappings;
    private String parentId;
    private String contractTemplatePath;
    private String contractTemplateVersion;
    private Instant contractTemplateUpdatedAt;
    private Instant createdAt;
    private String roleManagementURL;



    public static Optional<PartyRole> getPartyRole(String productRoleCode, EnumMap<PartyRole, ProductRoleInfo> roleMappings) {
        return getPartyRole(productRoleCode, roleMappings, EnumSet.allOf(PartyRole.class));
    }

    public static Optional<PartyRole> getPartyRole(String productRoleCode, EnumMap<PartyRole, ProductRoleInfo> roleMappings, EnumSet<PartyRole> partyRoleWhiteList) {
        return roleMappings.entrySet().stream()
                .filter(entry -> partyRoleWhiteList.contains(entry.getKey()))
                .filter(entry -> entry.getValue().getRoles().stream().anyMatch(productRole -> productRole.getCode().equals(productRoleCode)))
                .map(Map.Entry::getKey)
                .findAny();
    }

    public static Optional<String> getLabel(String productRoleCode, EnumMap<PartyRole, ProductRoleInfo> roleMappings) {
        return roleMappings.values().stream()
                .flatMap(productRoleInfo -> productRoleInfo.getRoles().stream())
                .filter(productRole -> productRole.getCode().equals(productRoleCode))
                .findAny()
                .map(ProductRoleInfo.ProductRole::getLabel);
    }


}
