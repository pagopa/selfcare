package it.pagopa.selfcare.dashboard.model.user_groups;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus;
import it.pagopa.selfcare.dashboard.model.product.ProductUserResource;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserGroupResource {
    @Schema(description = "${swagger.dashboard.user-group.model.id}")
    private String id;

    @Schema(description = "${swagger.dashboard.user-group.model.institutionId}")
    private String institutionId;

    @Schema(description = "${swagger.dashboard.user-group.model.productId}")
    private String productId;

    @Schema(description = "${swagger.dashboard.user-group.model.name}")
    private String name;

    @Schema(description = "${swagger.dashboard.user-group.model.description}")
    private String description;

    @Schema(description = "${swagger.dashboard.user-group.model.status}")
    private UserGroupStatus status;

    @Schema(description = "${swagger.dashboard.user-group.model.members}")
    private List<ProductUserResource> members;

    @Schema(description = "${swagger.dashboard.user-group.model.createdAt}")
    private Instant createdAt;

    @Schema(description = "${swagger.dashboard.user-group.model.createdBy}")
    private PlainUserResource createdBy;

    @Schema(description = "${swagger.dashboard.user-group.model.modifiedAt}")
    private Instant modifiedAt;

    @Schema(description = "${swagger.dashboard.user-group.model.modifiedBy}")
    private PlainUserResource modifiedBy;

}
