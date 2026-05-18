package it.pagopa.selfcare.onboarding.service;

import org.openapi.quarkus.user_json.model.SendMailDto;

public interface UserNotificationService {
  void sendMailRequest(String userId, SendMailDto sendMailDto);
}
