package it.pagopa.selfcare.mscore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.core.ExternalService;
import it.pagopa.selfcare.mscore.model.institution.GeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionBillingResponse;
import it.pagopa.selfcare.mscore.web.model.institution.InstitutionResponse;
import it.pagopa.selfcare.mscore.web.model.mapper.InstitutionMapperCustom;
import it.pagopa.selfcare.mscore.web.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.mscore.web.model.onboarding.OnboardedProducts;
import it.pagopa.selfcare.mscore.web.util.CustomExceptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.mscore.constant.GenericError.*;

@Slf4j
@RestController
@RequestMapping(value = "/external/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "External")
public class ExternalController {

    private final ExternalService externalService;
    private final InstitutionResourceMapper institutionResourceMapper;

    public ExternalController(ExternalService externalService, InstitutionResourceMapper institutionResourceMapper) {
        this.externalService = externalService;
        this.institutionResourceMapper = institutionResourceMapper;
    }

    /**
     * The function returns institution Data from its externalId
     *
     * @param  externalId externalId
     *
     * @return InstitutionResponse
     * * Code: 200, Message: successful operation, DataType: InstitutionResponse
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Institution not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.external.institution}", description = "${swagger.mscore.external.institution}")
    @GetMapping(value = "/{externalId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitutionResponse> getByExternalId(@Parameter(description = "${swagger.mscore.institutions.model.externalId}")
                                                               @PathVariable("externalId") String externalId) {
        CustomExceptionMessage.setCustomMessage(GET_INSTITUTION_BY_EXTERNAL_ID_ERROR);
        Institution institution = externalService.getInstitutionByExternalId(externalId);
        return ResponseEntity.ok().body(institutionResourceMapper.toInstitutionResponse(institution));
    }


    /**
     * The function returns billing institution Data from its externalId
     *
     * @param externalId String
     * @param productId  String
     *
     * @return InstitutionBillingResponse
     * * Code: 200, Message: successful operation, DataType: InstitutionBillingResponse
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Institution Billing not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.external.institution.billing}", description = "${swagger.mscore.external.institution.billing}")
    @GetMapping(value = "/{externalId}/products/{productId}/billing", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<InstitutionBillingResponse> getBillingInstitutionByExternalId(@Parameter(description = "${swagger.mscore.institutions.model.externalId}")
                                                                                        @PathVariable("externalId") String externalId,
                                                                                        @Parameter(description = "${swagger.mscore.institutions.model.productId}")
                                                                                        @PathVariable("productId") String productId) {
        CustomExceptionMessage.setCustomMessage(INSTITUTION_BILLING_ERROR);
        Institution institution = externalService.retrieveInstitutionProduct(externalId, productId);
        return ResponseEntity.ok().body(InstitutionMapperCustom.toInstitutionBillingResponse(institution, productId));
    }


    /**
     * The function returns institution related products given externalId
     *
     * @param externalId String
     * @param states List<String>
     *
     * @return OnboardedProducts
     * * Code: 200, Message: successful operation, DataType: OnboardedProducts
     * * Code: 400, Message: Invalid ID supplied, DataType: Problem
     * * Code: 404, Message: Institution Billing not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.external.institution.products}", description = "${swagger.mscore.external.institution.products}")
    @GetMapping(value = "/{externalId}/products", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<OnboardedProducts> retrieveInstitutionProductsByExternalId(@Parameter(description = "${swagger.mscore.institutions.model.externalId}")
                                                                                     @PathVariable("externalId") String externalId,
                                                                                     @Parameter(explode = Explode.TRUE, schema = @Schema(implementation = RelationshipState.class))
                                                                                     @RequestParam(value = "states", required = false) List<RelationshipState> states) {
        CustomExceptionMessage.setCustomMessage(GET_PRODUCTS_ERROR);
        List<Onboarding> page = externalService.retrieveInstitutionProductsByExternalId(externalId, states);
        return ResponseEntity.ok(InstitutionMapperCustom.toOnboardedProducts(page));
    }

    /**
     * The function returns geographic taxonomies related to institution
     *
     * @param externalId String
     *
     * @return List
     * * Code: 200, Message: successful operation, DataType: List<GeographicTaxonomies>
     * * Code: 404, Message: GeographicTaxonomies or Institution not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.external.geotaxonomies}", description = "${swagger.mscore.external.geotaxonomies}")
    @GetMapping(value = "/{externalId}/geotaxonomies", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<List<GeographicTaxonomies>> retrieveInstitutionGeoTaxonomiesByExternalId(@Parameter(description = "${swagger.mscore.institutions.model.externalId}")
                                                                                                   @PathVariable("externalId") String externalId) {
        CustomExceptionMessage.setCustomMessage(RETRIEVE_GEO_TAXONOMIES_ERROR);
        List<GeographicTaxonomies> list = externalService.retrieveInstitutionGeoTaxonomiesByExternalId(externalId);
        return ResponseEntity.ok(list);
    }

    /**
     * The function return an institution given institution internal id
     *
     * @param ids List
     * @return List
     *
     * * Code: 200, Message: successful operation, DataType: List<InstitutionResponse>
     * * Code: 404, Message: GeographicTaxonomies or Institution not found, DataType: Problem
     */
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.institution}", description = "${swagger.mscore.institution}", operationId = "retrieveInstitutionByIdsUsingGET")
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<List<InstitutionResponse>> retrieveInstitutionByIds(@Parameter(description = "${swagger.mscore.institutions.model.internalIds}")
                                                                                  @RequestParam("ids") List<String> ids) {
        CustomExceptionMessage.setCustomMessage(GET_INSTITUTION_BY_ID_ERROR);
        List<Institution> institutions = externalService.retrieveInstitutionByIds(ids);
        return ResponseEntity.ok().body(institutions.stream()
                .map(institutionResourceMapper::toInstitutionResponse)
                .collect(Collectors.toList()));
    }
}
