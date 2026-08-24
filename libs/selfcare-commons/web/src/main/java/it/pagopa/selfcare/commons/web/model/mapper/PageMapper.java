package it.pagopa.selfcare.commons.web.model.mapper;

import it.pagopa.selfcare.commons.web.model.Page;

public class PageMapper {

    public static <T> Page<T> map(org.springframework.data.domain.Page<T> page) {
        Page<T> result = null;
        if (page != null) {
            result = new Page<>();
            result.setContent(page.getContent());
            result.setTotalElements(page.getTotalElements());
            result.setTotalPages(page.getTotalPages());
            result.setNumber(page.getNumber());
            result.setSize(page.getSize());
        }
        return result;
    }

}
