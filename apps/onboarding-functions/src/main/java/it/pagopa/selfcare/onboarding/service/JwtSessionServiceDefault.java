package it.pagopa.selfcare.onboarding.service;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import it.pagopa.selfcare.onboarding.config.TokenConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;

@ApplicationScoped
public class JwtSessionServiceDefault implements JwtSessionService {

    @Inject
    TokenConfig tokenConfig;
    @RestClient
    @Inject
    UserApi userRegistryApi;

    private static final String PRIVATE_KEY_HEADER_TEMPLATE = "-----BEGIN %s-----";
    private static final String PRIVATE_KEY_FOOTER_TEMPLATE = "-----END %s-----";
    private final Logger logger = LoggerFactory.getLogger(JwtSessionServiceDefault.class.getName());

    @Override
    public String createJwt(String userId) {
        try {
            PrivateKey privateKey = getPrivateKey(tokenConfig.signingKey());
            UserResource userResource = userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userId);
            return Jwts.builder()
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(new Date())
                    .setIssuer(tokenConfig.issuer())
                    .setExpiration(Date.from(new Date().toInstant().plus(Duration.parse(tokenConfig.duration()))))
                    .claim("family_name", userResource.getFamilyName().getValue())
                    .claim("fiscal_number", userResource.getFiscalCode())
                    .claim("name", userResource.getName().getValue())
                    .claim("uid", userId)
                    .signWith(SignatureAlgorithm.RS256, privateKey)
                    .setHeaderParam(JwsHeader.KEY_ID, tokenConfig.kid())
                    .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                    .compact();
        } catch (Exception e) {
            logger.error("Impossible to create jwt token. Error: {}", e.getMessage(), e);
            return null;
        }
    }

    private PrivateKey getPrivateKey(String signingKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        boolean isRsa = signingKey.contains("RSA");
        String privateKeyEnvelopName = (isRsa ? "RSA " : "") + "PRIVATE KEY";
        String privateKeyPEM = signingKey
                .replace("\r", "")
                .replace("\n", "")
                .replace(String.format(PRIVATE_KEY_HEADER_TEMPLATE, privateKeyEnvelopName), "")
                .replace(String.format(PRIVATE_KEY_FOOTER_TEMPLATE, privateKeyEnvelopName), "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeySpec keySpec;
        if (isRsa) {
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(encoded);
            keySpec = new RSAPrivateCrtKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent(),
                    rsaPrivateKey.getPrivateExponent(),
                    rsaPrivateKey.getPrime1(),
                    rsaPrivateKey.getPrime2(),
                    rsaPrivateKey.getExponent1(),
                    rsaPrivateKey.getExponent2(),
                    rsaPrivateKey.getCoefficient());

        } else {
            keySpec = new PKCS8EncodedKeySpec(encoded);
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}
