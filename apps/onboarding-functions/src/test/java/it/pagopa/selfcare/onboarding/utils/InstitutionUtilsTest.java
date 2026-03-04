package it.pagopa.selfcare.onboarding.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class InstitutionUtilsTest {

  public static final String INSTITUTION_TYPE_DEFAULT = "DEFAULT";

  @Test
  void getCurrentInstitutionTypeTest() {
    // given
    Onboarding onboarding = createDummyOnboarding();

    // when
    String result = InstitutionUtils.getCurrentInstitutionType(onboarding);

    // then
    assertEquals("PSP", result);
  }

  @Test
  void getCurrentInstitutionTypeTest_shouldReturnDefault_whenInstitutionTypeIsNull() {
    // given
    Onboarding onboarding = createDummyOnboarding();
    onboarding.getInstitution().setInstitutionType(null);

    // when
    String result = InstitutionUtils.getCurrentInstitutionType(onboarding);

    // then
    assertEquals(INSTITUTION_TYPE_DEFAULT, result);
  }

  @Test
  void getCurrentInstitutionTypeTest_shouldReturnDefault_whenInstitutionIsNotNull() {
    // given
    Onboarding onboarding = createDummyOnboarding();
    onboarding.setInstitution(new Institution());
    // when
    String result = InstitutionUtils.getCurrentInstitutionType(onboarding);

    // then
    assertEquals(INSTITUTION_TYPE_DEFAULT, result);
  }

  @Test
  void getCurrentInstitutionTypeTest_shouldReturnDefault_whenInstitutionIsNull() {
    // given
    Onboarding onboarding = createDummyOnboarding();
    onboarding.setInstitution(null);
    // when
    String result = InstitutionUtils.getCurrentInstitutionType(onboarding);

    // then
    assertEquals(INSTITUTION_TYPE_DEFAULT, result);
  }

  private Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.randomUUID().toString());
    onboarding.setProductId("prod-pagopa");

    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setSubunitCode("subunitCode");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));
    return onboarding;
  }
}
