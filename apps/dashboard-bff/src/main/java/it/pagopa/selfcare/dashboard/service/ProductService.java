package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.Map;

public interface ProductService {

    Map<PartyRole, ProductRoleInfo> getProductRoles(String productId, String institutionType);

    ProductRolePermissionsList getMyPermissions(String userId);

}
