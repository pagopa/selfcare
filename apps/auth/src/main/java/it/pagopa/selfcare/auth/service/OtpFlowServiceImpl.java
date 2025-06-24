package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.model.FeatureFlagEnum;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.model.otp.OtpFeatureFlag;
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
import org.openapi.quarkus.internal_json.model.UserResource;

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
  public Uni<Optional<OtpFlow>> handleOtpFlow(UserClaims userClaims) {
    Optional<OtpFlow> emptyOtpFlow = Optional.empty();
    if (FeatureFlagEnum.NONE.equals(otpFeatureFlag.getFeatureFlag())) {
      return Uni.createFrom().item(emptyOtpFlow);
    }
    if (FeatureFlagEnum.BETA.equals(otpFeatureFlag.getFeatureFlag())) {
      if (otpFeatureFlag.isOtpForced(userClaims.getFiscalCode())) {
        userClaims.setSameIdp(Boolean.FALSE);
      } else {
        return Uni.createFrom().item(emptyOtpFlow);
      }
    }
    return userService
        .getUserInfo(userClaims)
        .onFailure(GeneralUtils::checkNotFoundException)
        .recoverWithNull()
        .map(Optional::ofNullable)
        .onFailure()
        .transform(
            failure ->
                new InternalException(
                    "Cannot get User Info on External Internal APIs:" + failure.toString()))
        .map(o -> o.map(UserResource::getEmail))
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
                                                            .map(Optional::of)
                                                        /*
                                                         * A previous PENDING OTP flow is found so we must ask for the same flow to be completed by user
                                                         */
                                                        : otpFlow
                                                                .getStatus()
                                                                .equals(OtpStatus.PENDING)
                                                            ? Uni.createFrom()
                                                                .item(Optional.of(otpFlow))
                                                            // a previous COMPLETED OTP flow with
                                                            // sameIdp=true is found. No OTP flow
                                                            // needed.
                                                            : Uni.createFrom().item(emptyOtpFlow))
                                            .orElse(
                                                !userClaims.getSameIdp()
                                                    // This is the first time a user is asked for an
                                                    // OTP flow
                                                    ? createAndSendOtp(
                                                            userClaims.getUid(), institutionalEmail)
                                                        .map(Optional::of)
                                                    : Uni.createFrom().item(emptyOtpFlow))))
                    // User is not present on Selfcare so we can proceed without OTP Flow
                    .orElse(Uni.createFrom().item(emptyOtpFlow)));
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
                createNewOtpFlow(userId, otp, email)
                    .onFailure(WebApplicationException.class)
                    .transform(GeneralUtils::extractExceptionFromWebAppException)
                    .chain(
                        otpFlow ->
                            otpNotificationService
                                .sendOtpEmail(userId, email, otp)
                                .replaceWith(otpFlow)));
  }

  @Override
  public Uni<OtpFlow> createNewOtpFlow(String userId, String otp, String email) {
    return Uni.createFrom()
        .item(OffsetDateTime.now())
        .map(
            now ->
                OtpFlow.builder()
                    .uuid(UUID.randomUUID().toString())
                    .userId(userId)
                    .attempts(0)
                    .notificationEmail(email)
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
