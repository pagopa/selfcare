package it.pagopa.selfcare.dashboard.connector.model.product;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.dashboard.model.product.BackOfficeConfigurations;
import it.pagopa.selfcare.dashboard.model.product.ProductOnBoardingStatus;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleInfo;
import it.pagopa.selfcare.dashboard.model.product.ProductStatus;
import lombok.Data;

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
    private ProductOnBoardingStatus onBoardingStatus;
    private ProductStatus status;
    private boolean delegable;
    private boolean invoiceable;
    private String identityTokenAudience;
    private EnumMap<PartyRole, ProductRoleInfo> roleMappings;
    private Map<String, BackOfficeConfigurations> backOfficeEnvironmentConfigurations;


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
