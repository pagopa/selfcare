package it.pagopa.selfcare.user.controller.response.product;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionProducts {

    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    private List<Product> products;
}