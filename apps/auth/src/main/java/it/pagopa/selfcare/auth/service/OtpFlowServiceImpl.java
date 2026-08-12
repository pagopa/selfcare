package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.response.OidcExchangeOtpResponse;
import it.pagopa.selfcare.auth.controller.response.OtpForbiddenCode;
import it.pagopa.selfcare.auth.controller.response.TokenResponse;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.ConflictException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.OtpForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpBetaUser;
import it.pagopa.selfcare.auth.model.otp.OtpDailyLimit;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.repository.OtpFlowRepository;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpFlowServiceImpl implements OtpFlowService {

  private final UserService userService;
  private final OtpNotificationService otpNotificationService;
  private final SessionService sessionService;
  private final OtpFlowRepository otpFlowRepository;

  @Inject
  OtpFeatureFlag otpFeatureFlag;

  @Inject
  OtpDailyLimit otpLimitConfig;

  @ConfigProperty(name = "auth-ms.retry.min-backoff")
  Integer retryMinBackOff;

  @ConfigProperty(name = "auth-ms.retry.max-backoff")
  Integer retryMaxBackOff;

  @ConfigProperty(name = "auth-ms.retry")
  Integer maxRetry;

  @ConfigProperty(name = "otp.duration")
  Integer otpDuration;

  @ConfigProperty(name = "otp.max.attempts")
  Integer otpMaxAttempts;

  @Override
  public Uni<Optional<OtpInfo>> handleOtpFlow(UserClaims userClaims, String tenantId) {
    Optional<OtpInfo> emptyOtpInfo = Optional.empty();
    String forcedEmail = null;
    if (FeatureFlagEnum.NONE.equals(otpFeatureFlag.getFeatureFlag())) {
      return Uni.createFrom().item(emptyOtpInfo);
    }
    if (FeatureFlagEnum.BETA.equals(otpFeatureFlag.getFeatureFlag())) {
      Optional<OtpBetaUser> maybeOtpBetaUser =
          otpFeatureFlag.getOtpBetaUser(userClaims.getFiscalCode());
      if (maybeOtpBetaUser.isEmpty()) {
        return Uni.createFrom().item(emptyOtpInfo);
      }
      OtpBetaUser betaUser = maybeOtpBetaUser.get();
      if (betaUser.getForceOtp()) {
        userClaims.setSameIdp(betaUser.getSameIdp());
        forcedEmail = betaUser.getForcedEmail();
      }
    }
    Optional<String> maybeForcedEmail = Optional.ofNullable(forcedEmail);

    return userService
            .getUserInfoEmail(userClaims.getUid())
            .onFailure(GeneralUtils::checkNotFoundException)
            .recoverWithNull()
            .map(Optional::ofNullable)
            .onFailure()
            .transform(
                    failure ->
                            new InternalException(
                                    "Cannot get User Info Email on External Internal APIs:" + failure))
            .map(optionalEmail -> optionalEmail.map(maybeForcedEmail::orElse))
            .chain(
                    maybeUserEmail ->
                            maybeUserEmail
                                    .map(email -> handleUserOtpFlow(userClaims, email, tenantId))
                                    .orElseGet(() -> Uni.createFrom().item(Optional.empty())));
  }

  private Uni<Optional<OtpInfo>> handleUserOtpFlow(
      UserClaims userClaims, String institutionalEmail, String tenantId) {

    return findLastOtpFlowByUserId(userClaims.getUid(), tenantId)
            .map(Optional::ofNullable)
            .onFailure()
            .transform(
                    failure ->
                            new InternalException("Cannot get last OTP Flow:" + failure))
            .chain(
                    maybeLastOtpFlow ->
                            maybeLastOtpFlow
                                    .map(flow -> handleExistingOtpFlow(flow, userClaims, institutionalEmail, tenantId))
                                    .orElseGet(() -> handleMissingOtpFlow(userClaims, institutionalEmail, tenantId)));
  }

  private Uni<Optional<OtpInfo>> handleExistingOtpFlow(
          OtpFlow otpFlow,
          UserClaims userClaims,
          String institutionalEmail,
          String tenantId) {

    return OtpUtils.isNewOtpFlowRequired(
                    otpFlow,
                    userClaims.getSameIdp(),
                    otpLimitConfig.getDailyLimit(),
                    () -> otpFlowRepository.countTodayDistinctUsers(tenantId))
            .chain(isRequired ->
                    isRequired
                            ? createAndSendOtp(userClaims.getUid(), institutionalEmail, tenantId)
                            .map(flow -> Optional.of(new OtpInfo(flow.getUuid(), institutionalEmail)))
                            : checkPendingOtpFlow(otpFlow, institutionalEmail));
  }

  private static Uni<Optional<OtpInfo>> checkPendingOtpFlow(OtpFlow otpFlow, String institutionalEmail) {
    return otpFlow.getStatus().equals(OtpStatus.PENDING)
            ? Uni.createFrom().item(Optional.of(new OtpInfo(otpFlow.getUuid(), institutionalEmail)))
            : Uni.createFrom().item(Optional.empty());
  }


  private Uni<Optional<OtpInfo>> handleMissingOtpFlow(
          UserClaims userClaims,
          String institutionalEmail,
          String tenantId) {

    return OtpUtils.isOtpRequiredWithMissingOtpFlow(
                    userClaims.getSameIdp(),
                    otpLimitConfig.getDailyLimit(),
                    () -> otpFlowRepository.countTodayDistinctUsers(tenantId))
            .chain(isRequired ->
                    isRequired
                            ? createAndSendOtp(userClaims.getUid(), institutionalEmail, tenantId)
                            .map(flow -> Optional.of(new OtpInfo(flow.getUuid(), institutionalEmail)))
                            : Uni.createFrom().item(Optional.empty()));
  }

  /**
   * This method is used to create a new OTP Flow and to send a mail notification containing an OTP
   * that the user must provide in order to complete authentication flow
   *
   * @param userId the user unique id (provided by PDV)
   * @param email the user's institutional email
   * @return a new Otp Flow
   */
  private Uni<OtpFlow> createAndSendOtp(String userId, String email, String tenantId) {
    return Uni.createFrom()
        .item(OtpUtils::generateOTP)
        .chain(
            otp ->
                createNewOtpFlow(userId, otp, tenantId)
                    .onFailure(WebApplicationException.class)
                    .transform(GeneralUtils::extractExceptionFromWebAppException)
                    .chain(
                        otpFlow ->
                            otpNotificationService
                                .sendOtpEmail(userId, email, otp)
                                .replaceWith(otpFlow)));
  }

  @Override
  public Uni<OtpFlow> createNewOtpFlow(String userId, String otp, String tenantId) {
    return Uni.createFrom()
        .item(OffsetDateTime.now())
        .map(
            now ->
                OtpFlow.builder()
                    .uuid(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .userId(userId)
                    .attempts(0)
                    .otp(DigestUtils.md5Hex(otp))
                    .status(OtpStatus.PENDING)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(otpDuration))
                    .build())
        .chain(otpFlowRepository::persist);
  }

  @Override
  public Uni<OtpFlow> findLastOtpFlowByUserId(String userId, String tenantId) {
    return otpFlowRepository.findLastOtpFlowByUserId(userId, tenantId);
  }

  private Uni<Optional<OtpFlow>> findOtpFlowByUuid(String uuid, String tenantId) {
    return otpFlowRepository.findOtpFlowByUuid(uuid, tenantId);
  }

  private Uni<Long> updateOtpFlow(
      String uuid, String tenantId, OtpStatus newStatus, Boolean attemptsIncrement) {
    return otpFlowRepository.updateOtpFlow(uuid, tenantId, newStatus, attemptsIncrement);
  }

  private Uni<Long> updateOtpFlowVerification(String uuid, String tenantId, OtpStatus newStatus) {
    return updateOtpFlow(uuid, tenantId, newStatus, true);
  }

  private Uni<String> handleOtpVerification(OtpFlow otpFlow, String hashedOtp, String tenantId) {
    if (otpFlow.getExpiresAt().isBefore(OffsetDateTime.now())) {
      return Uni.createFrom().failure(new ConflictException("Otp is expired"));
    }

    if (otpFlow.getStatus() != OtpStatus.PENDING) {
      return Uni.createFrom().failure(new ConflictException("Otp is in a final state"));
    }
    boolean maxAttemptsAlreadyReached = otpFlow.getAttempts() >= otpMaxAttempts;

    if (maxAttemptsAlreadyReached) {
      return Uni.createFrom()
          .failure(
              new OtpForbiddenException(
                  "Max attempts reached", OtpForbiddenCode.CODE_002, 0, otpFlow.getStatus()));
    }

    boolean isReachedMaxOnCurrentAttempt = otpFlow.getAttempts() + 1 >= otpMaxAttempts;
    if (!otpFlow.getOtp().equals(hashedOtp)) {
      OtpStatus newStatus = isReachedMaxOnCurrentAttempt ? OtpStatus.REJECTED : otpFlow.getStatus();
      Integer remainingAttempts = otpMaxAttempts - (otpFlow.getAttempts() + 1);
      return updateOtpFlowVerification(otpFlow.getUuid(), tenantId, newStatus)
          .onFailure()
          .transform(failure -> new InternalException("Cannot update OtpFlow"))
          .chain(
              () ->
                  Uni.createFrom()
                      .failure(
                          !isReachedMaxOnCurrentAttempt
                              ? new OtpForbiddenException(
                                  "Wrong Otp Code",
                                  OtpForbiddenCode.CODE_001,
                                  remainingAttempts,
                                  newStatus)
                              : new OtpForbiddenException(
                                  "Max attempts reached",
                                  OtpForbiddenCode.CODE_002,
                                  0,
                                  newStatus)));
    }
    return userService
        .getUserClaimsFromPdv(otpFlow.getUserId())
        .onFailure(GeneralUtils::checkIfIsRetryableException)
        .retry()
        .withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff))
        .atMost(maxRetry)
        .onFailure(WebApplicationException.class)
        .transform(GeneralUtils::extractExceptionFromWebAppException)
        .map(
            userClaims -> {
              userClaims.setTenantId(tenantId);
              return userClaims;
            })
        .chain(sessionService::generateSessionToken)
        .chain(
            sessionToken ->
                updateOtpFlowVerification(otpFlow.getUuid(), tenantId, OtpStatus.COMPLETED)
                    .onFailure()
                    .transform(
                        failure -> new InternalException("Cannot verify OTP:" + failure.toString()))
                    .replaceWith(sessionToken));
  }

  @Override
  public Uni<TokenResponse> verifyOtp(String otpUid, String otp, String tenantId) {
    return Uni.createFrom()
        .item(DigestUtils.md5Hex(otp))
        .chain(
            hashOtp ->
                findOtpFlowByUuid(otpUid, tenantId)
                    .chain(
                        maybeOtpFlow ->
                            maybeOtpFlow
                                .map(
                                    otpFlow ->
                                        handleOtpVerification(otpFlow, hashOtp, tenantId)
                                            .map(TokenResponse::new))
                                .orElse(
                                    Uni.createFrom()
                                        .failure(
                                            new ResourceNotFoundException(
                                                "Cannot find OtpFlow")))));
  }

  private Uni<OtpInfo> handleOtpResend(OtpFlow oldOtpFlow, String tenantId) {
    if (oldOtpFlow.getStatus() != OtpStatus.PENDING) {
      return Uni.createFrom().failure(new ConflictException("Otp is expired or in a final state"));
    }
    return userService
        .getUserClaimsFromPdv(oldOtpFlow.getUserId())
        .onFailure()
        .transform(
            failure -> new InternalException("Cannot get User from PDV" + failure.toString()))
        .chain(
            userClaims ->
                userService
                    .getUserInfoEmail(oldOtpFlow.getUserId())
                    .onFailure(GeneralUtils::checkNotFoundException)
                    .recoverWithNull()
                    .map(Optional::ofNullable)
                    .onFailure()
                    .transform(
                        failure ->
                            new InternalException(
                                "Cannot get User Info Email on External Internal APIs:"
                                    + failure.toString()))
                    .chain(
                        maybeUserEmail ->
                            maybeUserEmail
                                .map(
                                    institutionalEmail ->
                                        createAndSendOtp(userClaims.getUid(), institutionalEmail, tenantId)
                                            .chain(
                                                createdOtpFlow ->
                                                    // Fire & Forget update old otp flow status
                                                    updateOtpFlow(
                                                            oldOtpFlow.getUuid(),
                                                            tenantId,
                                                            OtpStatus.REJECTED,
                                                            false)
                                                        .replaceWith(createdOtpFlow)
                                                        .onFailure()
                                                        .recoverWithItem(createdOtpFlow)
                                                        .map(
                                                            newOtpFlow ->
                                                                OtpInfo.builder()
                                                                    .institutionalEmail(
                                                                        institutionalEmail)
                                                                    .uuid(newOtpFlow.getUuid())
                                                                    .build())))
                                .orElse(
                                    Uni.createFrom()
                                        .failure(new ConflictException("User not found")))));
  }

  @Override
  public Uni<OidcExchangeOtpResponse> resendOtp(String otpUid, String tenantId) {
    return findOtpFlowByUuid(otpUid, tenantId)
        .chain(
            maybeOtpFlow ->
                maybeOtpFlow
                    .map(
                        otpFlow ->
                            handleOtpResend(otpFlow, tenantId)
                                .map(
                                    newOtpInfo ->
                                        new OidcExchangeOtpResponse(
                                            newOtpInfo.getUuid(),
                                            OtpUtils.maskEmail(
                                                newOtpInfo.getInstitutionalEmail()))))
                    .orElse(
                        Uni.createFrom()
                            .failure(new ResourceNotFoundException("Cannot find OtpFlow"))));
  }
}
