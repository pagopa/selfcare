package it.pagopa.selfcare.product.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedFileTypesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedFileTypes {

  String HTML = "text/html";
  String PDF = "application/pdf";

  String message() default "File type not allowed";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] value();
}
