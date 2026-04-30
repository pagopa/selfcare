package it.pagopa.selfcare.user_group.model;

import lombok.Data;

import java.util.List;

@Data
public class UserGroupEntityPageable {
    private List<UserGroupEntity> content;
    private Integer totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number;
}
