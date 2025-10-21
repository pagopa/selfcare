package it.pagopa.selfcare.iam.repository;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Accumulators;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class UserPermissionsRepository {

  @Inject
  ReactiveMongoClient mongoClient;

  /**
   * Aggregazione per estrarre i permessi di un utente per un prodotto specifico
   */
  public Uni<UserPermissions> getUserPermissions(String uid, String permission, String productId) {
    List<Bson> pipeline = Arrays.asList(
      Aggregates.match(Filters.eq("_id", uid)),
      Aggregates.unwind("$productRoles"),
      Aggregates.match(Filters.eq("productRoles.productId", productId)),
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

      return getCollection()
      .aggregate(pipeline, Document.class)
      .collect().first()
      .map(this::documentToUserPermissions)
      .onItem().ifNull().failWith(() -> 
        new ResourceNotFoundException("Permission not found"));
  }

  /**
   * Versione senza filtro productId - restituisce tutti i permessi dell'utente
   */
  public Uni<Map<String, Set<String>>> getAllUserPermissions(String email) {
    List<Bson> pipeline = Arrays.asList(
      Aggregates.match(Filters.eq("_id", email)),
      Aggregates.unwind("$productRoles"),
      Aggregates.unwind("$productRoles.roles"),
      Aggregates.lookup("roles", "productRoles.roles", "_id", "roleDetails"),
      Aggregates.unwind("$roleDetails"),
      Aggregates.group(
        "$productRoles.productId",
        Accumulators.addToSet("permissions", "$roleDetails.permissions")
      ),
      Aggregates.project(
        Projections.fields(
          Projections.computed("productId", "$_id"),
          Projections.computed("permissions", 
            new Document("$reduce", new Document()
              .append("input", "$permissions")
              .append("initialValue", new ArrayList<>())
              .append("in", new Document("$concatArrays", Arrays.asList("$$value", "$$this")))
            )
          ),
          Projections.excludeId()
        )
      )
    );

    return getCollection()
      .aggregate(pipeline, Document.class)
      .collect().asList()
      .map(docs -> docs.stream()
        .collect(Collectors.toMap(
          doc -> doc.getString("productId"),
          doc -> new HashSet<>((List<String>) doc.get("permissions"))
        ))
      );
  }

  private ReactiveMongoCollection<Document> getCollection() {
    return mongoClient.getDatabase("selcIam")
      .getCollection("userClaims");
  }

  private UserPermissions documentToUserPermissions(Document doc) {
    if (doc == null) return null;
    
    return UserPermissions.builder()
      .email(doc.getString("email"))
      .uid(doc.getString("uid"))
      .productId(doc.getString("productId"))
      .permissions((List<String>) doc.get("permissions"))
      .build();
  }
}
