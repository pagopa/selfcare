package it.pagopa.selfcare.auth.util;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.model.OtpStatus;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public class OtpUtils {

  private OtpUtils() {}

  private static final SecureRandom random = new SecureRandom();

  public static String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return email;
    }

    String[] parts = email.split("@", 2);
    String username = parts[0];
    String domain = parts[1];

    String[] nameParts = username.split("\\.");

    StringBuilder maskedUsername = new StringBuilder();
    for (int i = 0; i < nameParts.length; i++) {
      maskedUsername.append(maskPart(nameParts[i]));
      if (i < nameParts.length - 1) {
        maskedUsername.append(".");
      }
    }

    return maskedUsername + "@" + domain;
  }

  private static String maskPart(String part) {
    if (part.length() <= 2) {
      return part.charAt(0) + "*";
    }

    return part.charAt(0) + "*".repeat(part.length() - 2) + part.charAt(part.length() - 1);
  }

  public static String generateOTP() {
    StringBuilder otp = new StringBuilder(6);
    for (int i = 0; i < 6; i++) {
      int digit = random.nextInt(10); // da 0 a 9
      otp.append(digit);
    }
    return otp.toString();
  }

  /**
   * User has completed a previous otp flow, and now he's challenged for a new otp due to Idp change
   * (or) previous otp flow expirationDate is past so we must ask for a new OTP Flow (or) previous
   * otp flow is in a final KO Status [EXPIRED, REJECTED]
   *
   * @param lastOtpFlow: The last otp flow related to a user
   * @param sameIdp: A boolean indicating if a user has changed its IdP since last login
   * @return a Boolean indicating if this user requires a brand new OtpFlow
   */
  public static Uni<Boolean> isNewOtpFlowRequired(OtpFlow lastOtpFlow, Boolean sameIdp, Integer limit) {

    if (Boolean.FALSE.equals(sameIdp)) {
      boolean isPendingAndNotExpired = lastOtpFlow.getStatus().equals(OtpStatus.PENDING) && lastOtpFlow.getExpiresAt().isAfter(OffsetDateTime.now());
      return isPendingAndNotExpired ? Uni.createFrom().item(false) : Uni.createFrom().item(true);
    }

    List<OtpStatus> otpStatusFinalKoStatuses = List.of(OtpStatus.EXPIRED, OtpStatus.REJECTED);
    boolean isOtpExpired = !lastOtpFlow.getStatus().equals(OtpStatus.COMPLETED)
            && lastOtpFlow.getExpiresAt().isBefore(OffsetDateTime.now());
    boolean isLastOtpKO = otpStatusFinalKoStatuses.contains(lastOtpFlow.getStatus());

    if (isOtpExpired || isLastOtpKO) {
      return Uni.createFrom().item(true);
    }

    return isPeriodicOtpRequiredWithLastOpt(lastOtpFlow, limit);
  }

  public static Uni<Boolean> isPeriodicOtpRequiredWithLastOpt(OtpFlow lastOtpFlow, Integer limit) {

      boolean isCompleted = lastOtpFlow.getStatus().equals(OtpStatus.COMPLETED);
      boolean isOlderThanSixMonths = lastOtpFlow.getCreatedAt().isBefore(OffsetDateTime.now().minusMonths(6));

      if (!(isCompleted && isOlderThanSixMonths)) {
        return Uni.createFrom().item(false);
      }

      return isPeriodicOtpRequired(limit);
    }

  public static Uni<Boolean> isOtpRequiredWithMissingOtpFlow(Boolean sameIdp, Integer limit) {
    if (Boolean.FALSE.equals(sameIdp)) {
      return Uni.createFrom().item(true);
    }
    return isPeriodicOtpRequired(limit);
  }

  public static Uni<Boolean> isPeriodicOtpRequired(Integer limit) {
    if (limit == 0) {
      return Uni.createFrom().item(false);
    }

    if (limit < 0) {
      return Uni.createFrom().item(true);
    }

    return otpCountTodayDistinctUsers()
            .map(count ->{
              log.info("OTP count is: {}", count);
              return count < limit;
            });
  }

  private static Uni<Long> otpCountTodayDistinctUsers() {

    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime startOfDay = now.toLocalDate()
            .atStartOfDay()
            .atOffset(now.getOffset());

    return OtpFlow.<OtpFlow>find("createdAt >= ?1", startOfDay.toInstant())
            .list()
            .map(list -> list.stream()
                    .map(OtpFlow::getUserId)
                    .distinct()
                    .count());
  }

}
