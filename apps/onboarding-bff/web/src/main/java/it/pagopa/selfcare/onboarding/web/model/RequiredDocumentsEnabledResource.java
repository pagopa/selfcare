package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentsEnabledResource {

    @Schema(description = "True when the (productId, institutionType, origin) triplet has "
            + "required-documents configured on product-ms, false otherwise.")
    private boolean requiredDocumentsEnabled;

}

