package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.constant.SelfCareRole;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.OnboardedProductFilter;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static it.pagopa.selfcare.user.constant.CustomError.USER_INSTITUTION_NOT_FOUND_ERROR;
import static it.pagopa.selfcare.user.constant.CustomError.USER_NOT_FOUND_ERROR;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserInstitutionService userInstitutionService;
    private final UserUtils userUtils;

    @Override
    public Uni<Boolean> hasPermission(String institutionId, String productId, PermissionTypeEnum permission, String userId) {
        log.trace("hasPermission start");
        log.debug("hasPermission institutionId = {}, productId = {}, permission = {}", institutionId, productId, permission);

        return retrievePerson(userId, productId, institutionId)
                .onItem().transform(userInstitution -> PermissionTypeEnum.ANY.equals(permission) || checkProductRole(userInstitution, productId, permission))
                .onFailure(ResourceNotFoundException.class).recoverWithItem(false);
    }

    private boolean checkProductRole(UserInstitution userInstitution, String productId, PermissionTypeEnum permission) {
        return userInstitution.getProducts().stream()
                .anyMatch(product -> permission.name().equalsIgnoreCase(SelfCareRole.valueOf(product.getRole().name()).getSelfCareAuthority().name()) &&
                        (StringUtils.isBlank(productId) || product.getProductId().equalsIgnoreCase(productId)));
    }

    private Uni<UserInstitution> retrievePerson(String userId, String productId, String institutionId) {
        var userInstitutionFilters = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build().constructMap();
        var productFilters = OnboardedProductFilter.builder().productId(productId).build().constructMap();
        Map<String, Object> queryParameter = userUtils.retrieveMapForFilter(userInstitutionFilters, productFilters);
        return userInstitutionService.retrieveFirstFilteredUserInstitution(queryParameter)
                .onItem().ifNull().failWith(() -> {
                    log.error(String.format(USER_INSTITUTION_NOT_FOUND_ERROR.getMessage(), userId, institutionId));
                    return new ResourceNotFoundException(String.format(USER_INSTITUTION_NOT_FOUND_ERROR.getMessage(), userId, institutionId), USER_NOT_FOUND_ERROR.getCode());
                });
    }
}
