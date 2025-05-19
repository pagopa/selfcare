package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserInstitutionService userInstitutionService;

    @Override
    public Uni<Boolean> hasPermission(String institutionId, String productId, PermissionTypeEnum permission, String userId) {
        log.trace("hasPermission start");
        log.debug("check permission {} for userId: {}, institutionId: {} and productId: {}", permission, userId, institutionId, productId);

        return userInstitutionService.existsValidUserProduct(userId, institutionId, productId, permission);
    }
}
