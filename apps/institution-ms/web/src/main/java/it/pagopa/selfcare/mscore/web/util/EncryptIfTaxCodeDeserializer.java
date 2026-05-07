package it.pagopa.selfcare.mscore.web.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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

public class EncryptIfTaxCodeDeserializer extends JsonDeserializer<String> {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern CF_PATTERN = Pattern.compile("[A-Za-z]");

    public EncryptIfTaxCodeDeserializer() {
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isBlank()) {
            return null;
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
            return value;
        }

        // otherwise check the value on userRegistry
        UserRegistryConnector userRegistryConnector = SpringContext.getBean(UserRegistryConnector.class);

        if (!UUID_PATTERN.matcher(value).matches() && CF_PATTERN.matcher(value).find()) {
            try {
                User user = userRegistryConnector.getUserByFiscalCode(value);
                return user != null ? user.getId() : value;
            } catch (ResourceNotFoundException | FeignException.NotFound e) {
                // 404: user not found →  return the original taxCode
                return value;
            }
        }

        return value;
    }
}
