package it.pagopa.selfcare.onboarding.dto;

import it.pagopa.selfcare.product.entity.Product;
import lombok.Data;

@Data
public class SendMailInput {
    Product product;
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