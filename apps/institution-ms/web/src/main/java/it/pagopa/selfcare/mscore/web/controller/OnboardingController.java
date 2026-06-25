package it.pagopa.selfcare.mscore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.mscore.constant.GenericError;
import it.pagopa.selfcare.mscore.core.OnboardingService;
import it.pagopa.selfcare.mscore.model.onboarding.VerifyOnboardingFilters;
import it.pagopa.selfcare.mscore.web.util.CustomExceptionMessage;
import it.pagopa.selfcare.mscore.web.util.EncryptedTaxCodeParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/onboarding")
@Tag(name = "Onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * The function verify onboarding status of given product and institution
     *
     * @param externalId String
     * @param productId  String
     * @return no content
     * * Code: 204, Message: successful operation, DataType: TokenId
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "${swagger.mscore.onboarding.verify}", description = "${swagger.mscore.onboarding.verify}", operationId = "#verifyOnboardingInfoUsingHEAD_1")
    @RequestMapping(method = {RequestMethod.HEAD}, value = "/institution/{externalId}/products/{productId}")
    @Deprecated
    public ResponseEntity<Void> verifyOnboardingInfo(@Parameter(description = "${swagger.mscore.institutions.model.externalId}")
                                                     @PathVariable(value = "externalId") String externalId,
                                                     @Parameter(description = "${swagger.mscore.institutions.model.productId}")
                                                     @PathVariable(value = "productId") String productId) {
        CustomExceptionMessage.setCustomMessage(GenericError.ONBOARDING_VERIFICATION_ERROR);
        onboardingService.verifyOnboardingInfo(externalId, productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    /**
     * The function verify onboarding status of given product and subunit of institution
     *
     * @param taxCode     String
     * @param subunitCode String
     * @param productId   String
     * @return no content
     * * Code: 204, Message: successful operation, DataType: TokenId
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "${swagger.mscore.onboarding.verify}", description = "${swagger.mscore.onboarding.verify}")
    @RequestMapping(method = {RequestMethod.HEAD}, value = "")
    @Deprecated
    public ResponseEntity<Void> verifyOnboardingInfo(@Parameter(description = "${swagger.mscore.institutions.model.taxCode}", required = true, in = ParameterIn.QUERY)
                                                     @EncryptedTaxCodeParam(required = true) String taxCode,
                                                     @Parameter(description = "${swagger.mscore.institutions.model.subunitCode}")
                                                     @RequestParam(value = "subunitCode", required = false) String subunitCode,
                                                     @Parameter(description = "${swagger.mscore.institutions.model.productId}")
                                                     @RequestParam(value = "productId") String productId) {
        CustomExceptionMessage.setCustomMessage(GenericError.ONBOARDING_VERIFICATION_ERROR);
        onboardingService.verifyOnboardingInfoSubunit(taxCode, subunitCode, productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "${swagger.mscore.onboarding.verify}", description = "${swagger.mscore.onboarding.verify}")
    @RequestMapping(method = {RequestMethod.HEAD}, value = "/verify")
    public ResponseEntity<Void> verifyOnboardingInfoByFilters(@Parameter(description = "${swagger.mscore.institutions.model.productId}")
                                                       @RequestParam(value = "productId") String productId,
                                                       @Parameter(description = "${swagger.mscore.institutions.model.externalId}", in = ParameterIn.QUERY)
                                                       @EncryptedTaxCodeParam String externalId,
                                                       @Parameter(description = "${swagger.mscore.institutions.model.taxCode}", in = ParameterIn.QUERY)
                                                       @EncryptedTaxCodeParam String taxCode,
                                                       @Parameter(description = "${swagger.mscore.institutions.model.origin}")
                                                       @RequestParam(value = "origin", required = false) String origin,
                                                       @Parameter(description = "${swagger.mscore.institutions.model.originId}", in = ParameterIn.QUERY)
                                                       @EncryptedTaxCodeParam String originId,
                                                       @Parameter(description = "${swagger.mscore.institutions.model.subunitCode}")
                                                       @RequestParam(value = "subunitCode", required = false) String subunitCode) {
        CustomExceptionMessage.setCustomMessage(GenericError.ONBOARDING_VERIFICATION_ERROR);
        onboardingService.verifyOnboardingInfoByFilters(new VerifyOnboardingFilters(productId, externalId, taxCode, origin, originId, subunitCode));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
