package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import java.util.Objects;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class InstitutionUtils {

  public static String getCurrentInstitutionType(Onboarding onboarding) {
    String institutionType = "DEFAULT";

    if (Objects.nonNull(onboarding.getInstitution())
        && Objects.nonNull(onboarding.getInstitution().getInstitutionType())) {
      institutionType = onboarding.getInstitution().getInstitutionType().name();
    }

    return institutionType;
  }
}
