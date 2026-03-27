package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;

/**
 * Request DTO for saving visura for merchant.
 * Contains all the data needed to generate the attachment without external calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVisuraRequest {
    @RestForm("onboardingId")
    @PartType(MediaType.TEXT_PLAIN)
    @NotBlank
    private String onboardingId;
    @RestForm("filename")
    @PartType(MediaType.TEXT_PLAIN)
    @NotBlank
    private String filename;

    @RestForm("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    @NotNull
    private InputStream fileContent;
}
