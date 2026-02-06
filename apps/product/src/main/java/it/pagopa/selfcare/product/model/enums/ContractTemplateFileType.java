package it.pagopa.selfcare.product.model.enums;

import it.pagopa.selfcare.product.exception.InvalidRequestException;
import lombok.Getter;

@Getter
public enum ContractTemplateFileType {
  HTML("text/html", "html"),
  PDF("application/pdf", "pdf");

  private final String contentType;
  private final String extension;

  ContractTemplateFileType(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public static ContractTemplateFileType from(String value) {
    return switch (value) {
      case "HTML", "html" -> HTML;
      case "PDF", "pdf" -> PDF;
      default ->
          throw new InvalidRequestException("Invalid ContractTemplateFileType: " + value, "400");
    };
  }
}
