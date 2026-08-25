package it.pagopa.selfcare.commons.web.security;

import io.kubernetes.client.openapi.apis.AuthenticationV1Api;
import io.kubernetes.client.openapi.models.*;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.ServiceAccount;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JwtAuthenticationStrategy} based on Kubernates JWT
 */
@Slf4j
@Service
public class K8sJwtAuthenticationStrategy implements JwtAuthenticationStrategy {

    private static final String MDC_UID = "uid";

    private final AuthenticationV1Api apiClient;


    @Autowired
    public K8sJwtAuthenticationStrategy(AuthenticationV1Api apiClient) {
        log.trace("Initializing {}", K8sJwtAuthenticationStrategy.class.getSimpleName());
        this.apiClient = apiClient;
    }


    @Override
    public JwtAuthenticationToken authenticate(JwtAuthenticationToken authentication) throws AuthenticationException {
        log.trace("authenticate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate authentication = {}", authentication);

        ServiceAccount user;
        try {
            final V1TokenReview body = new V1TokenReviewBuilder()
                    .withSpec(new V1TokenReviewSpecBuilder()
                            .withToken(authentication.getCredentials())
                            .build())
                    .build();
            final V1TokenReview tokenReview = apiClient.createTokenReview(body, null, null, null);
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "{}", tokenReview);
            final Optional<V1TokenReviewStatus> tokenStatus = Optional.ofNullable(tokenReview)
                    .map(V1TokenReview::getStatus);
            Assert.isTrue(tokenStatus
                    .map(V1TokenReviewStatus::getAuthenticated)
                    .orElse(false), "Invalid token status");
            final Optional<V1UserInfo> userInfo = tokenStatus
                    .map(V1TokenReviewStatus::getUser);
            Optional<String> uid = userInfo
                    .map(V1UserInfo::getUid);
            uid.ifPresentOrElse(value -> MDC.put(MDC_UID, value),
                    () -> log.warn("uid claims is null"));
            user = ServiceAccount.builder(uid.orElse(null))
                    .username(userInfo.map(V1UserInfo::getUsername)
                            .orElse(null))
                    .build();

        } catch (Exception e) {
            MDC.remove(MDC_UID);
            throw new JwtAuthenticationException(e.getMessage(), e);
        }

        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(authentication.getCredentials(),
                user,
                null);
        authenticationToken.setDetails(authentication.getDetails());

        log.trace("authenticate end");
        return authenticationToken;
    }

}
