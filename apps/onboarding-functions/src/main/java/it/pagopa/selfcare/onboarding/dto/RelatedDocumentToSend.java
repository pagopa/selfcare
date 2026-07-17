package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.pagopa.selfcare.onboarding.utils.CustomOffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import it.pagopa.selfcare.onboarding.common.DocumentType;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedDocumentToSend {

    private String id;
    private DocumentType type;
    private String fileName;
    private String type;
    private String mimeType;
    @JsonSerialize(using = CustomOffsetDateTimeSerializer.class)
    private OffsetDateTime createdAt;
    private String filePath;
}
