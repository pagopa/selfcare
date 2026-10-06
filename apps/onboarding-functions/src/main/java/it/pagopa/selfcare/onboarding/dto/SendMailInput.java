package it.pagopa.selfcare.onboarding.dto;

import org.openapi.quarkus.product_json.model.ProductResponse;
import lombok.Data;

@Data
public class SendMailInput {
    ProductResponse product;
    String userRequestName;
    // Used in case of workflowType USER
    String previousManagerName;
    String managerName;
    String userRequestSurname;
    // Used in case of workflowType USER
    String previousManagerSurname;
    String managerSurname;
    String institutionName;
}
