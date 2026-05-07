package it.pagopa.selfcare.dashboard.model.user_groups;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserGroupRequestDto {
    private String name;
    private String description;
    private List<String> members;
}
