package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.FdUserNotificationToSend;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.NotificationUserType;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.List;

public interface UserNotificationService {
    Uni<UserNotificationToSend> sendKafkaNotification(UserNotificationToSend userNotificationToSend);

    Uni<Void> sendEmailNotification(UserResource user, UserInstitution institution, Product product, OnboardedProductState status, String productRole, String loggedUserName, String loggedUserSurname);

    Uni<Void> sendCreateUserNotification(String institutionDescription, List<String> roleLabels, UserResource userResource, UserInstitution userInstitution, Product product, LoggedUser loggedUser);
}