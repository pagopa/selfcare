package it.pagopa.selfcare.user_group.model;

import it.pagopa.selfcare.commons.web.model.Page;

public class PageOfUserGroupResource extends Page<UserGroupResource> {

    public PageOfUserGroupResource(Page<UserGroupResource> page) {
        setContent(page.getContent());
        setTotalElements(page.getTotalElements());
        setTotalPages(page.getTotalPages());
        setNumber(page.getNumber());
        setSize(page.getSize());
    }

}
