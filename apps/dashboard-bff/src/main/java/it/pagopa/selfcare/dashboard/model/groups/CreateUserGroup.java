package it.pagopa.selfcare.dashboard.model.groups;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserGroup {
    private String institutionId;
    private String productId;
    private String name;
    private String description;
    private List<String> members;
}
