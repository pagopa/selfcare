package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import java.util.Objects;

public class InstitutionUtils {

  public InstitutionUtils() {}

  public static String getCurrentInstitutionType(Onboarding onboarding) {
    String institutionType = ProductConfigUtils.DEFAULT_INSTITUTION_TYPE;

    if (Objects.nonNull(onboarding.getInstitution())
        && Objects.nonNull(onboarding.getInstitution().getInstitutionType())) {
      institutionType = onboarding.getInstitution().getInstitutionType().name();
    }

    return institutionType;
  }
}
