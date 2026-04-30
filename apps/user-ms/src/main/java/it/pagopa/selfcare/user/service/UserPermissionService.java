package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;

public interface UserPermissionService {
     Uni<Boolean> hasPermission(String institutionId, String productId, PermissionTypeEnum permission, String userId);
}
