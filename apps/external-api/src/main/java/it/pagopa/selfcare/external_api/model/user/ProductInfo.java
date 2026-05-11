package it.pagopa.selfcare.external_api.model.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInfo {
    private String id;
    private String role;
    private String productRole;
    private OffsetDateTime createdAt;
    private String status;
}
