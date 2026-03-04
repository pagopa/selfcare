package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.Product;
import java.util.Objects;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class InstitutionUtils {

  public static String getCurrentInstitutionType(Onboarding onboarding) {
    String institutionType = Product.CONTRACT_TYPE_DEFAULT;

    if (Objects.nonNull(onboarding.getInstitution())
        && Objects.nonNull(onboarding.getInstitution().getInstitutionType())) {
      institutionType = onboarding.getInstitution().getInstitutionType().name();
    }

    return institutionType;
  }
}
