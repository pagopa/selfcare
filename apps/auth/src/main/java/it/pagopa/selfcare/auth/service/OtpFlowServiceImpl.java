package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class OtpFlowServiceImpl implements OtpFlowService {

  private final UserService userService;
  private final OtpNotificationService otpNotificationService;
  @Inject OtpFeatureFlag otpFeatureFlag;

  @ConfigProperty(name = "otp.duration")
  Integer otpDuration;

  @Override
  public Uni<Optional<OtpInfo>> handleOtpFlow(UserClaims userClaims) {
    Optional<OtpInfo> emptyOtpInfo = Optional.empty();
    if (FeatureFlagEnum.NONE.equals(otpFeatureFlag.getFeatureFlag())) {
      return Uni.createFrom().item(emptyOtpInfo);
    }
    if (FeatureFlagEnum.BETA.equals(otpFeatureFlag.getFeatureFlag())) {
      if (otpFeatureFlag.isOtpForced(userClaims.getFiscalCode())) {
        userClaims.setSameIdp(Boolean.FALSE);
      } else {
        return Uni.createFrom().item(emptyOtpInfo);
      }
    }
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
                    // User is not present on Selfcare so we can proceed without OTP Flow
                    .orElse(Uni.createFrom().item(emptyOtpInfo)));
  }

  /**
   * This method is used to create a new OTP Flow and to send an mail notification containing an OTP
   * that the user must provide in order to complete authentication flow
   *
   * @param userId the user unique id (provided by PDV)
   * @param email the user's intitutional email
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
}
