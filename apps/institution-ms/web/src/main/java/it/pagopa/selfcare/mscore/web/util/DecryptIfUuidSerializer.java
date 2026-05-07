package it.pagopa.selfcare.mscore.web.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import feign.FeignException;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Pattern;

public class DecryptIfUuidSerializer extends JsonSerializer<String> {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public DecryptIfUuidSerializer() {
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isBlank()) {
            gen.writeNull();
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) authentication.getCredentials();

        String aud = null;
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getAudience() != null && !claims.getAudience().isEmpty()) {
                aud = claims.getAudience().get(0);
            }
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing JWT", e);
        }

        // if aud contains "pnpg" then return the entered value
        if (aud != null && (aud.toLowerCase().contains("pnpg") || aud.toLowerCase().contains("imprese.notifichedigitali"))) {
            gen.writeString(value);
            return;
        }

        // otherwise check the value on userRegistry
        UserRegistryConnector userRegistryConnector = SpringContext.getBean(UserRegistryConnector.class);

        if (UUID_PATTERN.matcher(value).matches()) {
            try {
                User user = userRegistryConnector.getUserByInternalIdWithFiscalCode(value);
                // If the user is not found in UserRegistry, return the original value
                gen.writeString(user != null ? user.getFiscalCode() : value);
            } catch (ResourceNotFoundException | FeignException.NotFound e) {
                // 404: user not found → return the original value
                gen.writeString(value);
            }
        } else {
            gen.writeString(value);
        }
    }
}
