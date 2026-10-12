package it.pagopa.selfcare.onboarding.model;

import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.model.dto.request.UserDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.openapi.quarkus.onboarding_json.model.GeographicTaxonomyDto;

import java.util.List;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.aggregates}")
public class AggregateInstitution {

    @NotNull(message = "taxCode is required")
    private String taxCode;
    @NotNull(message = "description is required")
    private String description;
    private String subunitCode;
    private String subunitType;
    @Schema(type = SchemaType.ARRAY, implementation = it.pagopa.selfcare.onboarding.model.dto.request.GeographicTaxonomyDto.class)
    private List<GeographicTaxonomyDto> geographicTaxonomies;
    private String address;
    private String zipCode;
    private String originId;
    private Origin origin;
    private List<UserDto> users;
    private String iban;
    private String recipientCode;
    private String vatNumber;
    private String digitalAddress;
    private String city;
    private String county;
    private String parentDescription;

}
