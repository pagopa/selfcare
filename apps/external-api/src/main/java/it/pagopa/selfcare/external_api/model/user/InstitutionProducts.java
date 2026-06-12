package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.external_api.model.token.ProductToken;
import lombok.Data;

import java.util.List;

@Data
public class InstitutionProducts {
    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    private List<ProductToken> products;
}
