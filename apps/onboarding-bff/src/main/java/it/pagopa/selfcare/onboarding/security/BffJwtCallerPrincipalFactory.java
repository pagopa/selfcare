package it.pagopa.selfcare.onboarding.security;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.KeyLocationResolver;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.auth.principal.PrincipalUtils;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Set;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

/**
 * Verifies the session tokens of the BFF with the rules of the Spring BFF it replaces: a valid
 * signature of the configured key, {@code exp}/{@code nbf} checked only when present, no
 * {@code iat}, audience or uid requirement, and only the {@code SPID} and {@code PAGOPA} issuers.
 *
 * <p>SmallRye JWT, and so the security SDK factory built on it, hard-requires {@code exp} (and
 * {@code iat}) with no configuration switch; this factory is the supported extension point to
 * relax it while keeping the same signature verification and key resolution ({@code
 * mp.jwt.verify.*}).
 */
@ApplicationScoped
@Alternative
@Priority(10)
public class BffJwtCallerPrincipalFactory extends JWTCallerPrincipalFactory {

    public static final String CLAIM_ISSUER = "iss";
    public static final String CLAIM_UID = "uid";
    public static final String UID_NOT_PROVIDED = "uid_not_provided";

    private static final Set<String> ISSUERS = Set.of("SPID", "PAGOPA");
    private static final String[] SIGNATURE_ALGORITHMS = {
        AlgorithmIdentifiers.RSA_USING_SHA256,
        AlgorithmIdentifiers.RSA_USING_SHA384,
        AlgorithmIdentifiers.RSA_USING_SHA512,
        AlgorithmIdentifiers.RSA_PSS_USING_SHA256,
        AlgorithmIdentifiers.RSA_PSS_USING_SHA384,
        AlgorithmIdentifiers.RSA_PSS_USING_SHA512
    };

    private Verifier verifier;

    /**
     * The only claim checks of the Spring BFF (jjwt): {@code exp} and {@code nbf}, each one only when
     * present. The jose4j defaults would also reject an {@code exp} before {@code iat} and any
     * audience, issuer or subject expectation.
     */
    private static final Validator EXPIRATION_AND_NOT_BEFORE_WHEN_PRESENT = context -> {
        JwtClaims claims = context.getJwtClaims();
        long now = System.currentTimeMillis();
        NumericDate expiration = claims.getExpirationTime();
        if (expiration != null && now > expiration.getValueInMillis()) {
            return "The token is expired";
        }
        NumericDate notBefore = claims.getNotBefore();
        if (notBefore != null && now < notBefore.getValueInMillis()) {
            return "The token is not valid yet";
        }
        return null;
    };

    @Override
    public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
        JwtContext context = verifierFor(authContextInfo).verify(token);
        JwtClaims claims = context.getJwtClaims();

        requireKnownIssuer(claims);
        String subject = String.valueOf(uidOrPlaceholder(claims));
        JWTAuthContextInfo subjectOfUid = new JWTAuthContextInfo(authContextInfo);
        subjectOfUid.setDefaultSubjectClaim(subject);
        PrincipalUtils.setClaims(claims, token, subjectOfUid);
        return new DefaultJWTCallerPrincipal(token, context.getJoseObjects().get(0).getHeader("typ"), claims);
    }

    private synchronized Verifier verifierFor(JWTAuthContextInfo authContextInfo) throws ParseException {
        Verifier current = verifier;
        if (current == null || current.source != authContextInfo) {
            current = new Verifier(authContextInfo);
            verifier = current;
        }
        return current;
    }

    private static void requireKnownIssuer(JwtClaims claims) throws ParseException {
        if (!(claims.getClaimValue(CLAIM_ISSUER) instanceof String issuer) || !ISSUERS.contains(issuer)) {
            throw new UnknownIssuerException();
        }
    }

    /** The uid of the Spring {@code SelfCareUser}: the claim, or the placeholder when not provided. */
    private static Object uidOrPlaceholder(JwtClaims claims) {
        Object uid = claims.getClaimValue(CLAIM_UID);
        if (uid == null) {
            claims.setClaim(CLAIM_UID, UID_NOT_PROVIDED);
            return UID_NOT_PROVIDED;
        }
        return uid;
    }

    /** The jose4j consumer bound to the context it was built from, created once per context. */
    private static final class Verifier {

        private final JWTAuthContextInfo source;
        private final JwtConsumer consumer;

        private Verifier(JWTAuthContextInfo source) throws ParseException {
            this.source = source;
            try {
                JwtConsumerBuilder builder = new JwtConsumerBuilder()
                        .setVerificationKeyResolver(new KeyLocationResolver(source))
                        .setJwsAlgorithmConstraints(new AlgorithmConstraints(
                                AlgorithmConstraints.ConstraintType.PERMIT, SIGNATURE_ALGORITHMS))
                        .setSkipAllDefaultValidators()
                        .registerValidator(EXPIRATION_AND_NOT_BEFORE_WHEN_PRESENT);
                if (source.isRelaxVerificationKeyValidation()) {
                    builder.setRelaxVerificationKeyValidation();
                }
                this.consumer = builder.build();
            } catch (Exception e) {
                throw new ParseException("The JWT verification key cannot be resolved", e);
            }
        }

        private JwtContext verify(String token) throws ParseException {
            try {
                return consumer.process(token);
            } catch (Exception e) {
                throw new ParseException("Token validation failed", e);
            }
        }
    }

    /** A token with a valid signature but an issuer the BFF does not know. */
    public static class UnknownIssuerException extends ParseException {

        public UnknownIssuerException() {
            super("Unknown issuer");
        }
    }
}
