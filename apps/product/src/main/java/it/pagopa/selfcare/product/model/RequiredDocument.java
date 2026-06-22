package it.pagopa.selfcare.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocument {

  @BsonProperty("id")
  private String id;
  private String name;
  private String labelKey;
  private boolean required;
  private String mimeType;
  private Integer maxDocumentsRequired;
  private RequiredDocumentFilter filter;
}
