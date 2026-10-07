package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class IpaInstitutionsSearchResource {

    private List<IpaInstitutionResource> items;
    private Long count;
}
