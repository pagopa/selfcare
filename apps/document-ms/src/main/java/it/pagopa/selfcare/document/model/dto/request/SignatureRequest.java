package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import lombok.Data;
import org.jboss.resteasy.reactive.PartType;

import java.io.File;
import java.util.List;

@Data
public class SignatureRequest {

    @FormParam("onboardingId")
    @NotBlank
    private String onboardingId;

    @FormParam("file")
    @NotNull
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private File file;

    @FormParam("fiscalCodes")
    @NotNull
    @PartType(MediaType.APPLICATION_JSON)
    private List<String> fiscalCodes;
}
