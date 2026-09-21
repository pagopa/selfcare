package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.base.security.SelfCareUser.SelfCareUserBuilder;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementation of {@link JwtAuthenticationStrategy} based on SPID JWT
 */
@Slf4j
@Service
public class PagopaJwtAuthenticationStrategy implements JwtAuthenticationStrategy {

    private static final String MDC_UID = "uid";
    private static final String CLAIMS_UID = "uid";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_SURNAME = "family_name";
    private static final String CLAIM_ISSUER = "iss";
    private static final String CLAIM_TENANT_ID = "tenant_id";

    private final JwtService jwtService;
    private final AuthoritiesRetriever authoritiesRetriever;
    private final TenantRegistry tenantRegistry;
    private final String defaultTenantId;


    @Autowired
    public PagopaJwtAuthenticationStrategy(
            JwtService jwtService,
            AuthoritiesRetriever authoritiesRetriever,
            ObjectProvider<TenantRegistry> tenantRegistryProvider,
            @Value("${tenant.default:PNPG}") String defaultTenantId) {
        log.trace("Initializing {}", PagopaJwtAuthenticationStrategy.class.getSimpleName());
        this.jwtService = jwtService;
        this.authoritiesRetriever = authoritiesRetriever;
        this.tenantRegistry =
                tenantRegistryProvider == null ? null : tenantRegistryProvider.getIfAvailable();
        this.defaultTenantId =
                org.springframework.util.StringUtils.hasText(defaultTenantId)
                        ? defaultTenantId
                        : "PNPG";
    }

    PagopaJwtAuthenticationStrategy(
            JwtService jwtService, AuthoritiesRetriever authoritiesRetriever) {
        this(jwtService, authoritiesRetriever, null, "PNPG");
    }


    @Override
    public JwtAuthenticationToken authenticate(JwtAuthenticationToken authentication) throws AuthenticationException {
        log.trace("authenticate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate authentication = {}", authentication);

        SelfCareUser user;
        String tenantId;
        try {
            Claims claims = tenantRegistry != null && tenantRegistry.isConfigured()
                    ? jwtService.getClaims(
                            authentication.getCredentials(), authentication.getTenantId())
                    : jwtService.getClaims(authentication.getCredentials());
            tenantId = tenantRegistry != null && tenantRegistry.isConfigured()
                    ? resolveTenant(authentication.getTenantId(), claims)
                    : authentication.getTenantId();
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate user with id = {}", claims.get(CLAIMS_UID, String.class));
            Optional<String> uid = Optional.ofNullable(claims.get(CLAIMS_UID, String.class));
            uid.ifPresentOrElse(value -> MDC.put(MDC_UID, value),
                    () -> log.warn("uid claims is null"));

            SelfCareUserBuilder userBuilder = SelfCareUser.builder(uid.orElse("uid_not_provided"))
                    .issuer(claims.get(CLAIM_ISSUER, String.class))
                    .email(claims.get(CLAIM_EMAIL, String.class));
            
            Optional.ofNullable(claims.get(CLAIM_NAME, String.class)).ifPresent(userBuilder::name);
            Optional.ofNullable(claims.get(CLAIM_SURNAME, String.class)).ifPresent(userBuilder::surname);
            
            user = userBuilder.build();        
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
        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(
                authentication.getCredentials(), user, authorities, tenantId);

        log.trace("authenticate end");
        return authenticationToken;
    }

    private String resolveTenant(String headerTenantId, Claims claims) {
        String claimTenantId = claims.get(CLAIM_TENANT_ID, String.class);
        String effectiveTenant =
                org.springframework.util.StringUtils.hasText(claimTenantId)
                        ? normalizeAndValidate(claimTenantId)
                        : normalizeAndValidate(defaultTenantId);
        String headerTenant = normalizeAndValidate(headerTenantId);
        if (!effectiveTenant.equals(headerTenant)) {
            throw new TenantValidationException();
        }
        return effectiveTenant;
    }

    private String normalizeAndValidate(String tenantId) {
        if (tenantRegistry != null && tenantRegistry.isConfigured()) {
            return tenantRegistry.normalizeAndValidate(tenantId);
        }
        if (!org.springframework.util.StringUtils.hasText(tenantId)) {
            throw new TenantValidationException();
        }
        String normalized = tenantId.trim().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("AR", "PNPG").contains(normalized)) {
            throw new TenantValidationException();
        }
        return normalized;
    }

}
