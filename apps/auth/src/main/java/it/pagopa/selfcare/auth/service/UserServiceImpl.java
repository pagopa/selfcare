package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.client.ExternalInternalUserApi;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.internal_json.model.SearchUserDto;
import org.openapi.quarkus.internal_json.model.UserInfoResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.FamilyNameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.NameCertifiableSchema;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;

import java.time.Duration;
import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private static final String SPID_FISCAL_NUMBER_PREFIX = "TINIT-";

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @RestClient @Inject UserApi userRegistryApi;

  @RestClient @Inject ExternalInternalUserApi externalInternalUserApi;

  @Override
  public Uni<UserClaims> patchUser(
      String fiscalNumber, String name, String familyName, Boolean sameIdp) {
    String fiscalCode = fiscalNumber.replace(SPID_FISCAL_NUMBER_PREFIX, "");
    SaveUserDto saveUserDto = new SaveUserDto();
    saveUserDto.name(
        NameCertifiableSchema.builder()
            .certification(NameCertifiableSchema.CertificationEnum.SPID)
            .value(name)
            .build());
    saveUserDto.familyName(
        FamilyNameCertifiableSchema.builder()
            .certification(FamilyNameCertifiableSchema.CertificationEnum.SPID)
            .value(familyName)
            .build());
    saveUserDto.fiscalCode(fiscalCode);
    return userRegistryApi
        .saveUsingPATCH(saveUserDto)
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .map(
            userId ->
                UserClaims.builder()
                    .uid(userId.getId().toString())
                    .fiscalCode(fiscalCode)
                    .name(name)
                    .familyName(familyName)
                    .sameIdp(sameIdp)
                    .build());
  }

  @Override
  public Uni<String> getUserInfoEmail(UserClaims userClaims) {
    SearchUserDto searchUserDto =
        SearchUserDto.builder()
            .fiscalCode(userClaims.getFiscalCode())
            .statuses(List.of(SearchUserDto.StatusesEnum.ACTIVE))
            .build();
    return externalInternalUserApi
        .v2getUserInfoUsingGET(searchUserDto)
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .map(UserInfoResource::getOnboardedInstitutions)
        .map(
            onboardedInstitutionResources ->
                onboardedInstitutionResources.stream()
                    .max(
                        (oi, oi2) ->
                            oi2.getProductInfo()
                                .getCreatedAt()
                                .compareTo(oi.getProductInfo().getCreatedAt())))
        .chain(
            maybeOnboardedInst ->
                maybeOnboardedInst
                    .map(onboardedInst -> Uni.createFrom().item(onboardedInst.getUserEmail()))
                    .orElse(
                        Uni.createFrom()
                            .failure(
                                new ResourceNotFoundException("Onboarded Institution Not Found"))));
  }
}
