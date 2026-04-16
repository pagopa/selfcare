package it.pagopa.selfcare.product.validator;

import it.pagopa.selfcare.product.util.HtmlUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Slf4j
public class AllowedFileTypesValidator
    implements ConstraintValidator<AllowedFileTypes, FileUpload> {

  private Set<String> allowedTypes;

  @Override
  public void initialize(AllowedFileTypes constraintAnnotation) {
    this.allowedTypes = Set.of(constraintAnnotation.value());
  }

  @Override
  public boolean isValid(
      FileUpload fileUpload, ConstraintValidatorContext constraintValidatorContext) {
    if (fileUpload == null) {
      return true; // Null values are handled by @NotNull if needed
    }

    final String contentType = fileUpload.contentType();
    if (!allowedTypes.contains(contentType)) {
      return false;
    }

    return switch (contentType) {
      case AllowedFileTypes.HTML -> HtmlUtils.isValidHTML(fileUpload.uploadedFile().toFile());
      case AllowedFileTypes.PDF ->
          false; // TODO: Implement security checks before allowing PDF files
      default -> false;
    };
  }
}
