package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.UserNotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_json.api.UserApi;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UserNotificationServiceImpl implements UserNotificationService {

  private static final Logger log = LoggerFactory.getLogger(UserNotificationServiceImpl.class);

  private final UserApi userApi;

  @Inject
  public UserNotificationServiceImpl(@RestClient UserApi userApi) {
    this.userApi = userApi;
  }

  @Override
  public void sendMailRequest(String userId, SendMailDto sendMailDto) {
    try {
      userApi.sendMailRequest(userId, sendMailDto);
    } catch (Exception e) {
      log.error("Impossible to send mail to user {}", userId, e);
    }
  }
}
