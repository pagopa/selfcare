package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
public class CreatedAtRequest {
    @NotBlank(message = "productId is mandatory")
    private String productId;
    @NotNull(message = "createdAt is mandatory")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdAt;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime activatedAt;
}
