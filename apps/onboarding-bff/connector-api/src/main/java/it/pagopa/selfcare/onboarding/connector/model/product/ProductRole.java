package it.pagopa.selfcare.onboarding.connector.model.product;

import lombok.Data;

import java.util.List;

@Data
public class ProductRole {

    private String code;
    private String label;
    private String productLabel;
    private String description;
    private List<String> multiroleGroups;
}

