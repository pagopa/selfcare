package it.pagopa.selfcare.mscore.web.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuid;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkInstitution {

    @NotNull
    private String id;

    @NotNull
    @DecryptIfUuid
    private String externalId;

    @NotNull
    private String origin;

    @NotNull
    @DecryptIfUuid
    private String originId;

    @NotNull
    private String description;

    @NotNull
    private String digitalAddress;

    @NotNull
    private String address;

    @NotNull
    private String zipCode;

    @NotNull
    @DecryptIfUuid
    private String taxCode;

    private List<AttributesResponse> attributes;

    @NotNull
    private Map<String, BulkProduct> products;
}
