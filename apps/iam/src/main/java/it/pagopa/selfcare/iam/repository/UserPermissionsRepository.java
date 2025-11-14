package it.pagopa.selfcare.iam.repository;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.iam.exception.ResourceNotFoundException;
import it.pagopa.selfcare.iam.model.UserPermissions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@ApplicationScoped
public class UserPermissionsRepository {

  @Inject
  ReactiveMongoClient mongoClient;

  /**
   * Aggregation query to extract a user's permissions for a specific product (optional).
   */
  public Uni<UserPermissions> getUserPermissions(String uid, String permission, String productId) {
    List<Bson> pipeline = new ArrayList<>();
    pipeline.add(Aggregates.match(Filters.eq("_id", uid)));
    pipeline.add(Aggregates.unwind("$productRoles"));

    List<String> productIds = Optional.ofNullable(productId).isPresent() ? List.of("ALL", productId) : List.of("ALL");
    pipeline.add(Aggregates.match(Filters.in("productRoles.productId", productIds)));

    List<Bson> pipelinePost = Arrays.asList(
      Aggregates.unwind("$productRoles.roles"),
      Aggregates.lookup(
        "roles",
        "productRoles.roles",
        "_id",
        "roleDetails"
      ),
      Aggregates.unwind("$roleDetails"),
      Aggregates.unwind("$roleDetails.permissions"),
      Aggregates.match(Filters.eq("roleDetails.permissions", permission)),
      Aggregates.group(
        new Document("uid", "$_id")
          .append("email", "$email")
          .append("productId", "$productRoles.productId"),
        Accumulators.addToSet("permissions", "$roleDetails.permissions")
      ),
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
          //     .append("in", new Document("$concatArrays", Arrays.asList("$$value", "$$this")))
          //   )
          // ),
          Projections.excludeId()
        )
      )
    );

    pipeline.addAll(pipelinePost);

    return getCollection()
      .aggregate(pipeline, Document.class)
      .collect().first()
      .map(this::documentToUserPermissions)
      .onItem().ifNull().failWith(() ->
        new ResourceNotFoundException("Permission not found"));
  }

  private ReactiveMongoCollection<Document> getCollection() {
    return mongoClient.getDatabase("selcIam")
      .getCollection("userClaims");
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
