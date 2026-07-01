package it.pagopa.selfcare.onboarding.web.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentsEnabledResource {

    @ApiModelProperty(value = "True when the (productId, institutionType, origin) triplet has "
            + "required-documents configured on product-ms, false otherwise.")
    private boolean requiredDocumentsEnabled;

}

