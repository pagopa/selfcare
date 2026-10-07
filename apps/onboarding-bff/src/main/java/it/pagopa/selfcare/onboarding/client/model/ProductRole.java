package it.pagopa.selfcare.onboarding.client.model;

import java.util.List;
import lombok.Data;

@Data
public class ProductRole {

    private String code;
    private String label;
    private String productLabel;
    private String description;
    private List<String> multiroleGroups;
}
