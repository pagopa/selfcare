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
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
import it.pagopa.selfcare.auth.model.otp.OtpInfo;
import it.pagopa.selfcare.auth.util.GeneralUtils;
import it.pagopa.selfcare.auth.util.OtpUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpFlowServiceImpl implements OtpFlowService {

  private final UserService userService;
  private final OtpNotificationService otpNotificationService;
  private final SessionService sessionService;

  @Inject OtpFeatureFlag otpFeatureFlag;

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
  public Uni<Optional<OtpInfo>> handleOtpFlow(UserClaims userClaims) {
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
        userClaims.setSameIdp(Boolean.FALSE);
        forcedEmail = betaUser.getForcedEmail();
      }
    }

    Optional<String> maybeForcedEmail = Optional.ofNullable(forcedEmail);
    return userService
        .getUserInfoEmail(userClaims)
        .onFailure(GeneralUtils::checkNotFoundException)
        .recoverWithNull()
        .map(Optional::ofNullable)
        .onFailure()
        .transform(
            failure ->
                new InternalException(
                    "Cannot get User Info Email on External Internal APIs:" + failure.toString()))
        .map(optionalEmail -> optionalEmail.map(maybeForcedEmail::orElse))
        .chain(
            maybeUserEmail ->
                maybeUserEmail
                    .map(
                        // User found with email
                        institutionalEmail ->
                            // check if last otp flow is present for user
                            findLastOtpFlowByUserId(userClaims.getUid())
                                .onFailure(GeneralUtils::checkNotFoundException)
                                .recoverWithNull()
                                .map(Optional::ofNullable)
                                .onFailure()
                                .transform(
                                    failure ->
                                        new InternalException(
                                            "Cannot get last OTP Flow:" + failure.toString()))
                                .chain(
                                    maybeLastOtpFlow ->
                                        maybeLastOtpFlow
                                            .map(
                                                otpFlow ->
                                                    OtpUtils.isNewOtpFlowRequired(
                                                            otpFlow, userClaims.getSameIdp())
                                                        ? createAndSendOtp(
                                                                userClaims.getUid(),
                                                                institutionalEmail)
                                                            .map(
                                                                createdOtpFlow ->
                                                                    Optional.of(
                                                                        new OtpInfo(
                                                                            createdOtpFlow
                                                                                .getUuid(),
                                                                            institutionalEmail)))
                                                        /*
                                                         * A previous PENDING OTP flow is found so we must ask for the same flow to be completed by user
                                                         */
                                                        : otpFlow
                                                                .getStatus()
                                                                .equals(OtpStatus.PENDING)
                                                            ? Uni.createFrom()
                                                                .item(
                                                                    Optional.of(
                                                                        new OtpInfo(
                                                                            otpFlow.getUuid(),
                                                                            institutionalEmail)))
                                                            // a previous COMPLETED OTP flow with
                                                            // sameIdp=true is found. No OTP flow
                                                            // needed.
                                                            : Uni.createFrom().item(emptyOtpInfo))
                                            .orElse(
                                                !userClaims.getSameIdp()
                                                    // This is the first time a user is asked for an
                                                    // OTP flow
                                                    ? createAndSendOtp(
                                                            userClaims.getUid(), institutionalEmail)
                                                        .map(
                                                            createdOtpFlow ->
                                                                Optional.of(
                                                                    new OtpInfo(
                                                                        createdOtpFlow.getUuid(),
                                                                        institutionalEmail)))
                                                    : Uni.createFrom().item(emptyOtpInfo))))
                    // User is not present on Self care so we can proceed without OTP Flow
                    .orElse(Uni.createFrom().item(emptyOtpInfo)));
  }

  /**
   * This method is used to create a new OTP Flow and to send a mail notification containing an OTP
   * that the user must provide in order to complete authentication flow
   *
   * @param userId the user unique id (provided by PDV)
   * @param email the user's institutional email
   * @return a new Otp Flow
   */
  private Uni<OtpFlow> createAndSendOtp(String userId, String email) {
    return Uni.createFrom()
        .item(OtpUtils::generateOTP)
        .chain(
            otp ->
                createNewOtpFlow(userId, otp)
                    .onFailure(WebApplicationException.class)
                    .transform(GeneralUtils::extractExceptionFromWebAppException)
                    .chain(
                        otpFlow ->
                            otpNotificationService
                                .sendOtpEmail(userId, email, otp)
                                .replaceWith(otpFlow)));
  }

  @Override
  public Uni<OtpFlow> createNewOtpFlow(String userId, String otp) {
    return Uni.createFrom()
        .item(OffsetDateTime.now())
        .map(
            now ->
                OtpFlow.builder()
                    .uuid(UUID.randomUUID().toString())
                    .userId(userId)
                    .attempts(0)
                    .otp(DigestUtils.md5Hex(otp))
                    .status(OtpStatus.PENDING)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(otpDuration))
                    .build())
        .chain(otpFlow -> OtpFlow.persist(otpFlow).map(v -> otpFlow));
  }

  @Override
  public Uni<OtpFlow> findLastOtpFlowByUserId(String userId) {
    final String userIdField = OtpFlow.Fields.userId.name();
    final String createdAtField = OtpFlow.Fields.createdAt.name();
    return OtpFlow.find(new Document(userIdField, userId), new Document(createdAtField, -1))
        .firstResult();
  }

  private Uni<Optional<OtpFlow>> findOtpFlowByUuid(String uuid) {
    return OtpFlow.find(new Document(OtpFlow.Fields.uuid.name(), uuid)).firstResultOptional();
  }

  private Uni<Long> updateOtpFlow(String uuid, OtpStatus newStatus, Boolean attemptsIncrement) {
    StringBuilder updateBuilder = new StringBuilder();
    updateBuilder.append("{");
    if (attemptsIncrement) {
      updateBuilder.append(" $inc': { 'attempts': 1 },");
    }
    updateBuilder.append(" '$set': { 'status': ?1, 'updatedAt': ?2 } }");
    return OtpFlow.update(
            updateBuilder.toString(), newStatus, Date.from(OffsetDateTime.now().toInstant()))
        .where("uuid", uuid);
  }

  private Uni<Long> updateOtpFlowVerification(String uuid, OtpStatus newStatus) {
    return updateOtpFlow(uuid, newStatus, true);
  }

  private Uni<String> handleOtpVerification(OtpFlow otpFlow, String hashedOtp) {
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
      return updateOtpFlowVerification(otpFlow.getUuid(), newStatus)
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
        .chain(sessionService::generateSessionToken)
        .chain(
            sessionToken ->
                updateOtpFlowVerification(otpFlow.getUuid(), OtpStatus.COMPLETED)
                    .onFailure()
                    .transform(
                        failure -> new InternalException("Cannot verify OTP:" + failure.toString()))
                    .replaceWith(sessionToken));
  }

  @Override
  public Uni<TokenResponse> verifyOtp(String otpUid, String otp) {
    return Uni.createFrom()
        .item(DigestUtils.md5Hex(otp))
        .chain(
            hashOtp ->
                findOtpFlowByUuid(otpUid)
                    .chain(
                        maybeOtpFlow ->
                            maybeOtpFlow
                                .map(
                                    otpFlow ->
                                        handleOtpVerification(otpFlow, hashOtp)
                                            .map(TokenResponse::new))
                                .orElse(
                                    Uni.createFrom()
                                        .failure(
                                            new ResourceNotFoundException(
                                                "Cannot find OtpFlow")))));
  }

  private Uni<OtpInfo> handleOtpResend(OtpFlow oldOtpFlow) {
    if (oldOtpFlow.getExpiresAt().isBefore(OffsetDateTime.now())
        || oldOtpFlow.getStatus() != OtpStatus.PENDING) {
      return Uni.createFrom().failure(new ConflictException("Otp is expired or in a final state"));
    }
    return userService
        .getUserClaimsFromPdv(oldOtpFlow.getUserId())
            .onFailure()
            .transform(
                    failure ->
                            new InternalException(
                                    "Cannot get User from PDV"
                                            + failure.toString()))
        .chain(
            userClaims ->
                userService
                    .getUserInfoEmail(userClaims)
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
                                        createAndSendOtp(userClaims.getUid(), institutionalEmail)
                                            .chain(
                                                createdOtpFlow ->
                                                    // Fire & Forget update old otp flow status
                                                    updateOtpFlow(
                                                            createdOtpFlow.getUuid(),
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
                                        .failure(new ConflictException("User not found"))))
        );
  }

  @Override
  public Uni<OidcExchangeOtpResponse> resendOtp(String otpUid) {
    return findOtpFlowByUuid(otpUid)
        .chain(
            maybeOtpFlow ->
                maybeOtpFlow
                    .map(
                        otpFlow ->
                            handleOtpResend(otpFlow)
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
