package it.pagopa.selfcare.document.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadAttachmentForm {
    @RestForm("file")
    @NotNull
    private File file;

    @RestForm("request")
    @PartType(MediaType.APPLICATION_JSON)
    @Valid
    private DocumentBuilderRequest request;
}
