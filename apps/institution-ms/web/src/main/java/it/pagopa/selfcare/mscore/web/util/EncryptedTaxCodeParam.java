package it.pagopa.selfcare.mscore.web.util;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptedTaxCodeParam {

    String value() default "";

    boolean required() default false;
}
