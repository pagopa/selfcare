package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.InstitutionType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMapping {
  private String role;
  private InstitutionType institutionType;
  private boolean multiroleAllowed;
  private List<String> phasesAdditionAllowed;
  private boolean skipUserCreation;
  private List<BackOfficeRole> backOfficeRoles;
}
