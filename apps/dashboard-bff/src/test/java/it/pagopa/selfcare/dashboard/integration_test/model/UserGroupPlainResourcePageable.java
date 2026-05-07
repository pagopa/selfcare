package it.pagopa.selfcare.dashboard.integration_test.model;

import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupPlainResource;
import lombok.Data;

import java.util.List;

@Data
public class UserGroupPlainResourcePageable {
    private int totalElements;
    private int totalPages;
    private int number;
    private int size;
    private List<UserGroupPlainResource> content;
}
