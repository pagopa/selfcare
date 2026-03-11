package it.pagopa.selfcare.user.model.notification;

import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.model.constants.QueueEvent;
import lombok.Builder;
import lombok.Getter;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Builder
@Getter
public class PrepareNotificationData {
    private UserInstitution userInstitution;
    private UserResource userResource;
    private Product product;
    private QueueEvent queueEvent;
    private UserResource loggedUserResource;
}