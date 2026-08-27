package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.Optional;

/**
 * Implementation of {@link JwtAuthenticationStrategy} based on SPID JWT
 */
@Slf4j
@Service
public class SpidJwtAuthenticationStrategy implements JwtAuthenticationStrategy {

    private static final String MDC_UID = "uid";
    private static final String CLAIMS_UID = "uid";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_SURNAME = "family_name";
    private static final String CLAIM_FISCAL_CODE = "fiscal_number";
    private static final String CLAIM_ISSUER = "iss";
    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String DEFAULT_TENANT_ID = "PNPG";
    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final Set<String> SUPPORTED_TENANTS = Set.of("AR", DEFAULT_TENANT_ID);

    private final JwtService jwtService;
    private final AuthoritiesRetriever authoritiesRetriever;


    @Autowired
    public SpidJwtAuthenticationStrategy(JwtService jwtService, AuthoritiesRetriever authoritiesRetriever) {
        log.trace("Initializing {}", SpidJwtAuthenticationStrategy.class.getSimpleName());
        this.jwtService = jwtService;
        this.authoritiesRetriever = authoritiesRetriever;
    }


    @Override
    public JwtAuthenticationToken authenticate(JwtAuthenticationToken authentication) throws AuthenticationException {
        log.trace("authenticate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate authentication = {}", authentication);

        SelfCareUser user;
        String tenantId;
        try {
            Claims claims = jwtService.getClaims(authentication.getCredentials());
            tenantId = resolveTenant(authentication.getTenantId(), claims);
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate user with id = {}", claims.get(CLAIMS_UID, String.class));
            Optional<String> uid = Optional.ofNullable(claims.get(CLAIMS_UID, String.class));
            uid.ifPresentOrElse(value -> MDC.put(MDC_UID, value),
                    () -> log.warn("uid claims is null"));

            user = SelfCareUser.builder(uid.orElse("uid_not_provided"))
                    .email(claims.get(CLAIM_EMAIL, String.class))
                    .name(claims.get(CLAIM_NAME, String.class))
                    .surname(claims.get(CLAIM_SURNAME, String.class))
                    .fiscalCode(claims.get(CLAIM_FISCAL_CODE, String.class))
                    .issuer(claims.get(CLAIM_ISSUER, String.class))
                    .build();

        } catch (TenantValidationException e) {
            MDC.remove(MDC_UID);
            throw e;
        } catch (Exception e) {
            MDC.remove(MDC_UID);
            throw new JwtAuthenticationException(e.getMessage(), e);
        }

        final Collection<GrantedAuthority> authorities;
        try {
            authorities = authoritiesRetriever.retrieveAuthorities();
        } catch (Exception e) {
            throw new AuthoritiesRetrieverException("An error occurred during authorities retrieval", e);
        }
        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(authentication.getCredentials(),
                user,
                authorities,
                tenantId);

        log.trace("authenticate end");
        return authenticationToken;
    }


    private String resolveTenant(String headerTenantId, Claims claims) {
        final Object rawClaimTenantId = claims.get(CLAIM_TENANT_ID);
        final String effectiveTenantId;
        if (rawClaimTenantId == null) {
            effectiveTenantId = DEFAULT_TENANT_ID;
        } else if (rawClaimTenantId instanceof String claimTenantId
                && StringUtils.hasText(claimTenantId)
                && SUPPORTED_TENANTS.contains(claimTenantId)) {
            effectiveTenantId = claimTenantId;
        } else {
            throw new TenantValidationException();
        }

        if (!StringUtils.hasText(headerTenantId)
                || !SUPPORTED_TENANTS.contains(headerTenantId)
                || !effectiveTenantId.equals(headerTenantId)) {
            log.warn("Tenant header {} does not match the verified JWT tenant", TENANT_HEADER);
            throw new TenantValidationException();
        }
        return effectiveTenantId;
    }

}
