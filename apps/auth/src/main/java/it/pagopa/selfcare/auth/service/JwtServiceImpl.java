package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeResponse;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.one_identity_json.api.TokenServerApisApi;
import org.openapi.quarkus.one_identity_json.api.TokenServerApisApi.CreateRequestTokenMultipartForm;
import org.openapi.quarkus.one_identity_json.model.TokenData;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

  @Inject JWTParser jwtParser;

  public Uni<JsonWebToken> parseJwt (String jwt) {
    try {
      JsonWebToken jsonWebToken = jwtParser.parse(jwt);
      return Uni.createFrom().item(jsonWebToken);
    } catch (ParseException failure) {
      return Uni.createFrom().failure(() -> new Exception("Cannot parse jwt", failure.getCause()));
    }
  }

  @Override
  public Uni<Map<String, String>> extractClaimsFromJwtToken(String jwt) {
    return this.parseJwt(jwt)
            .map(
            token ->
                token.getClaimNames().stream()
                    .collect(
                        Collectors.toMap(
                            Function.identity(),
                            claimName -> token.getClaim(claimName).toString())));
  }
}
