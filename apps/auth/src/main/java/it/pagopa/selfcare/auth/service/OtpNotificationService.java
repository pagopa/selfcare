package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;

public interface OtpNotificationService {
  Uni<Void> sendOtpEmail(String userId, String email, String otp);
}
