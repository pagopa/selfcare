package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.response.UserInfoResponse;
import it.pagopa.selfcare.user.conf.CurrentTenantProvider;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.mapper.UserInfoMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserInfoServiceDefault implements UserInfoService {

    /** Field carrying the owning tenant on {@code userInfo} (Step_0 sub-task 6). */
    private static final String TENANT_ID_FIELD = "tenantId";

    private final UserInfoMapper userInfoMapper;
    private final CurrentTenantProvider currentTenantProvider;

    /**
     * Restricts a lookup to the tenant validated for the current request.
     *
     * <p>Migration-phase semantics, matching the rest of the service: records written before the
     * discriminator existed carry no tenant and stay visible to both, and a call with no resolvable
     * tenant is left unscoped rather than made unsatisfiable.
     */
    private Document tenantScoped(Document query) {
        return currentTenantProvider
                .currentTenantId()
                .map(tenant -> new Document("$and", List.of(
                        query,
                        new Document("$or", List.of(
                                new Document(TENANT_ID_FIELD, tenant),
                                new Document(TENANT_ID_FIELD, null))))))
                .orElse(query);
    }

    @Override
    public Uni<UserInfoResponse> findById(String userId) {
        Uni<UserInfo> userInfo = UserInfo.find(tenantScoped(new Document("_id", userId))).firstResult();
        return userInfo
                .onItem().invoke(user -> log.info("Founded userInfo for userId: {}", userId))
                .onItem().transform(userInfoMapper::toResponse);
    }

}
