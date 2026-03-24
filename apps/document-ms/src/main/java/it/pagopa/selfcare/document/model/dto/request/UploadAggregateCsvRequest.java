package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

@Data
public class UploadAggregateCsvRequest {
    @RestForm("onboardingId")
    @PartType(MediaType.TEXT_PLAIN)
    @NotBlank
    private String onboardingId;

    @RestForm("productId")
    @PartType(MediaType.TEXT_PLAIN)
    @NotBlank
    private String productId;

    @RestForm("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    @NotNull
    private File csv;

}
