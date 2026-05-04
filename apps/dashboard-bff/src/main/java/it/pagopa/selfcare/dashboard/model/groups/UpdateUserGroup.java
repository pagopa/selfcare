package it.pagopa.selfcare.dashboard.model.groups;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserGroup {
    private String name;
    private String description;
    private List<String> members;
}
