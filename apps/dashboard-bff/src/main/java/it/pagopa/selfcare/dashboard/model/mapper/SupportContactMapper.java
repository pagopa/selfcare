package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.institution.SupportContact;
import it.pagopa.selfcare.dashboard.model.SupportContactResource;

public class SupportContactMapper {

    public static SupportContactResource toResource(SupportContact model) {
        SupportContactResource resource = null;
        if (model != null) {
            resource = new SupportContactResource();
            resource.setSupportEmail(model.getSupportEmail());
            resource.setSupportPhone(model.getSupportPhone());
        }
        return resource;
    }

}
