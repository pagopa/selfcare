package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstitutionListResponse {

    List<InstitutionManagementResponse> items;

}
