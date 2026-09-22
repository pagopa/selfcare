package it.pagopa.selfcare.onboarding.web.model;

import java.util.List;
import lombok.Data;

@Data
public class IpaInstitutionsSearchResource {

    private List<IpaInstitutionResource> items;
    private Long count;
}
