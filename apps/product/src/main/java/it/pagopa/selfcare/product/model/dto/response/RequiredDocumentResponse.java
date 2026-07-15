package it.pagopa.selfcare.product.model.dto.response;

import it.pagopa.selfcare.product.entity.StorageOrigin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocumentResponse {

  private String id;
  private String name;
  private String labelKey;
  private boolean required;
  private String mimeType;
  private Integer maxDocumentsRequired;
  private StorageOrigin storageOrigin;
}

