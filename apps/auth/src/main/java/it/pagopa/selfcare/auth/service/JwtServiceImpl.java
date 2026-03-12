package it.pagopa.selfcare.auth.service;

import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

  @Inject JWTParser jwtParser;

  private Uni<JsonWebToken> parseJwt(String jwt) {
    try {
      JsonWebToken jsonWebToken = jwtParser.parseOnly(jwt);
      return Uni.createFrom().item(jsonWebToken);
    } catch (ParseException failure) {
      log.error("Cannot parse jwt", failure.getCause());
      return Uni.createFrom().failure(() -> new Exception("Cannot parse jwt", failure.getCause()));
    }
  }

  @Override
  public Uni<Map<String, String>> extractClaimsFromJwtToken(String jwt) {
    return this.parseJwt(jwt)
        .onItem()
        .transformToUni(
            parsedJwt ->
                Optional.ofNullable(parsedJwt)
                    .map(token -> Uni.createFrom().item(token))
                    .orElse(Uni.createFrom().failure(new Exception("Parsed jwt is null"))))
        .map(
            token ->
                token.getClaimNames().stream()
                    .collect(
                        Collectors.toMap(
                            Function.identity(),
                            claimName -> token.getClaim(claimName).toString())));
  }
}
