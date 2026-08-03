package it.pagopa.selfcare.auth.service;

import io.smallrye.mutiny.Uni;
import org.openapi.quarkus.one_mail_json.model.EmailStatusItemResponseDTO;

public interface OtpNotificationService {
  Uni<String> sendOtpEmail(String userId, String email, String otp, String name);

  Uni<EmailStatusItemResponseDTO> getOtpMailInfo(String mailRequestId);
}
