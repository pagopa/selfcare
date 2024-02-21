package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import org.openapi.quarkus.user_registry_json.model.UserResource;

public interface UserNotificationService {
    Uni<UserNotificationToSend> sendKafkaNotification(UserNotificationToSend userNotificationToSend, String userId);
    Uni<Void> sendEmailNotification(UserResource user, UserInstitution institution, Product product, OnboardedProductState status, String loggedUserName, String loggedUserSurname);
}