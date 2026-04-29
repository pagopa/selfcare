package it.pagopa.selfcare.mscore.web.util;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import feign.FeignException;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.user.User;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.text.ParseException;
import java.util.regex.Pattern;

public class EncryptedTaxCodeParamResolver implements HandlerMethodArgumentResolver {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern CF_PATTERN = Pattern.compile("[A-Za-z]");

    private final UserRegistryConnector userRegistryConnector;

    public EncryptedTaxCodeParamResolver(UserRegistryConnector userRegistryConnector) {
        this.userRegistryConnector = userRegistryConnector;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(EncryptedTaxCodeParam.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws MissingServletRequestParameterException {

        EncryptedTaxCodeParam annotation = parameter.getParameterAnnotation(EncryptedTaxCodeParam.class);
        String paramName = (annotation != null && !annotation.value().isEmpty())
                ? annotation.value()
                : parameter.getParameterName();
        boolean required = annotation != null && annotation.required();

        assert paramName != null;
        String taxCode = webRequest.getParameter(paramName);

        if (!StringUtils.hasText(taxCode)) {
            if (required) {
                throw new MissingServletRequestParameterException(String.format("Missing required field %s", paramName), "0000");
            } else {
                return null;
            }
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
            return taxCode;
        }

        // otherwise check the value on userRegistry
        if (!UUID_PATTERN.matcher(taxCode).matches() && CF_PATTERN.matcher(taxCode).find()) {
            try {
                User user = userRegistryConnector.getUserByFiscalCode(taxCode);
                return user != null ? user.getId() : taxCode;
            } catch (ResourceNotFoundException | FeignException.NotFound e) {
                // 404: user not found →  return the original taxCode
                return taxCode;
            }
        }

        return taxCode;
    }
}
