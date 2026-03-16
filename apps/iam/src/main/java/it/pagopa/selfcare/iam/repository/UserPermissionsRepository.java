package it.pagopa.selfcare.iam.repository;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UnwindOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.exception.InternalException;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.ProductRole;
import it.pagopa.selfcare.iam.model.ProductRolePermissions;
import it.pagopa.selfcare.iam.model.UserPermissions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class UserPermissionsRepository {

  public static final String ALL = "ALL";
  @Inject ReactiveMongoClient mongoClient;

  @ConfigProperty(name = "quarkus.mongodb.database")
  String databaseName;

  /** Aggregation query to extract a user's permissions for a specific product (optional). */
  public Uni<UserPermissions> getUserPermissions(
      String uid, String permission, List<String> products) {
    List<Bson> pipeline = new ArrayList<>();
    pipeline.add(Aggregates.match(Filters.eq("_id", uid)));
    pipeline.add(Aggregates.unwind("$productRoles"));

    List<String> productIds =
        Optional.ofNullable(products).isPresent() && !products.isEmpty()
            ? Stream.concat(Stream.of(ALL), products.stream()).toList()
            : List.of(ALL);

    pipeline.add(Aggregates.match(Filters.in("productRoles.productId", productIds)));

    List<Bson> pipelinePost =
        Arrays.asList(
            Aggregates.unwind("$productRoles.roles"),
            Aggregates.lookup("roles", "productRoles.roles", "_id", "roleDetails"),
            Aggregates.unwind("$roleDetails"),
            Aggregates.unwind("$roleDetails.permissions"),
            Aggregates.match(Filters.eq("roleDetails.permissions", permission)),
            Aggregates.group(
                new Document("uid", "$_id")
                    .append("email", "$email")
                    .append("productId", "$productRoles.productId"),
                Accumulators.addToSet("permissions", "$roleDetails.permissions")),
            Aggregates.project(
                Projections.fields(
                    Projections.computed("email", "$_id.email"),
                    Projections.computed("uid", "$_id.uid"),
                    Projections.computed("productId", "$_id.productId"),
                    Projections.computed("permissions", "$permissions"),
                    // Projections.computed("permissions",
                    //   new Document("$reduce", new Document()
                    //     .append("input", "$permissions")
                    //     .append("initialValue", new ArrayList<>())
                    //     .append("in", new Document("$concatArrays", Arrays.asList("$$value",
                    // "$$this")))
                    //   )
                    // ),
                    Projections.excludeId())));

    pipeline.addAll(pipelinePost);

    return getCollection()
        .aggregate(pipeline, Document.class)
        .collect()
        .first()
        .map(this::documentToUserPermissions)
        .onItem()
        .ifNull()
        .failWith(() -> new ResourceNotFoundException("Permission not found"));
  }

  /** Aggregation query to extract a list of product, role and permissions for a specific user. */
  public Uni<List<ProductRolePermissions>> getUserProductRolePermissionsList(
      String uid, String productId) {
    List<Bson> pipeline = new ArrayList<>();
    pipeline.add(Aggregates.match(Filters.eq("_id", uid)));
    pipeline.add(Aggregates.unwind("$productRoles"));
    Optional.ofNullable(productId)
        .ifPresent(
            pid -> pipeline.add(Aggregates.match(Filters.eq("productRoles.productId", pid))));

    List<Bson> pipelinePost =
        Arrays.asList(
            Aggregates.unwind("$productRoles.roles"),
            Aggregates.lookup("roles", "productRoles.roles", "_id", "roleDetails"),
            Aggregates.unwind("$roleDetails"),
            Aggregates.project(
                Projections.fields(
                    Projections.computed("role", "$roleDetails._id"),
                    Projections.computed("group", "$roleDetails.group"),
                    Projections.computed("productId", "$productRoles.productId"),
                    Projections.computed("permissions", "$roleDetails.permissions"),
                    Projections.excludeId())));

    pipeline.addAll(pipelinePost);

    return getCollection()
        .aggregate(pipeline, ProductRolePermissions.class)
        .collect()
        .asList()
        .onFailure()
        .transform(
            failure ->
                new InternalException(
                    "Error retrieving product role permissions list: " + failure.toString()));
  }

  /** Aggregation query to extract a list of product, role for a specific user. */
  public Uni<List<ProductRole>> getUserProductRoles(String uid, String productId) {
    List<Bson> pipeline = new ArrayList<>();
    pipeline.add(Aggregates.match(Filters.eq("_id", uid)));
    pipeline.add(Aggregates.unwind("$productRoles"));
    Optional.ofNullable(productId)
        .ifPresent(
            pid -> pipeline.add(Aggregates.match(Filters.eq("productRoles.productId", pid))));

    List<Bson> pipelinePost =
        Arrays.asList(
            Aggregates.unwind("$productRoles.roles"),
            Aggregates.lookup("roles", "productRoles.roles", "_id", "roleDetails"),
            Aggregates.unwind("$roleDetails", new UnwindOptions().preserveNullAndEmptyArrays(true)),
            Aggregates.project(
                Projections.fields(
                    Projections.computed(
                        "role",
                        new Document(
                            "$ifNull", Arrays.asList("$roleDetails._id", "$productRoles.roles"))),
                    Projections.computed(
                        "group",
                        new Document("$ifNull", Arrays.asList("$roleDetails.group", null))),
                    Projections.computed("productId", "$productRoles.productId"),
                    Projections.excludeId())),
            Aggregates.group(
                "$productId",
                Accumulators.addToSet(
                    "roles", new Document("role", "$role").append("group", "$group"))),
            Aggregates.project(
                Projections.fields(
                    Projections.computed("productId", "$_id"),
                    Projections.include("roles"),
                    Projections.excludeId())));

    pipeline.addAll(pipelinePost);

    return getCollection()
        .aggregate(pipeline, Document.class)
        .collect()
        .asList()
        .map(docs -> docs.stream().map(this::documentToProductRole).toList())
        .onFailure()
        .transform(
            failure ->
                new InternalException("Error retrieving product role list: " + failure.toString()));
  }

  private ProductRole documentToProductRole(Document doc) {
    List<Document> roleDocs = (List<Document>) doc.get("roles");
    List<it.pagopa.selfcare.iam.model.Role> roles =
        roleDocs == null
            ? List.of()
            : roleDocs.stream()
                .map(
                    r ->
                        it.pagopa.selfcare.iam.model.Role.builder()
                            .role(r.getString("role"))
                            .group(r.getString("group"))
                            .build())
                .toList();
    return ProductRole.builder().productId(doc.getString("productId")).roles(roles).build();
  }

  private ReactiveMongoCollection<Document> getCollection() {
    return mongoClient.getDatabase(databaseName).getCollection("userClaims");
  }

  private UserPermissions documentToUserPermissions(Document doc) {
    if (doc == null) {
      return null;
    }

    return UserPermissions.builder()
        .email(doc.getString("email"))
        .uid(doc.getString("uid"))
        .productId(doc.getString("productId"))
        .permissions((List<String>) doc.get("permissions"))
        .build();
  }
}
