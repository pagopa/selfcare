package it.pagopa.selfcare.commons.web.security;

import io.jsonwebtoken.Claims;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.base.security.SelfCareUser.SelfCareUserBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
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

    private final JwtService jwtService;
    private final AuthoritiesRetriever authoritiesRetriever;


    @Autowired
    public PagopaJwtAuthenticationStrategy(JwtService jwtService, AuthoritiesRetriever authoritiesRetriever) {
        log.trace("Initializing {}", PagopaJwtAuthenticationStrategy.class.getSimpleName());
        this.jwtService = jwtService;
        this.authoritiesRetriever = authoritiesRetriever;
    }


    @Override
    public JwtAuthenticationToken authenticate(JwtAuthenticationToken authentication) throws AuthenticationException {
        log.trace("authenticate start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "authenticate authentication = {}", authentication);

        SelfCareUser user;
        try {
            Claims claims = jwtService.getClaims(authentication.getCredentials());
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
        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(authentication.getCredentials(),
                user,
                authorities);

        log.trace("authenticate end");
        return authenticationToken;
    }

}
