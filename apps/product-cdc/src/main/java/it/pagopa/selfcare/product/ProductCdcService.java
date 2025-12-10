package it.pagopa.selfcare.product;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.applicationinsights.TelemetryClient;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.product.conf.JacksonConfiguration;
import it.pagopa.selfcare.product.constant.ProductConstant;
import it.pagopa.selfcare.product.mapper.ProductMapper;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.TrackEventInput;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.*;

import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static java.util.Arrays.asList;

@Startup
@Slf4j
@ApplicationScoped
public class ProductCdcService {
  private final TelemetryClient telemetryClient;
  private final TableClient tableClient;
  private final String mongodbDatabase;
  private final ReactiveMongoClient mongoClient;
  private final ProductService productService;
  private final AzureBlobClient azureBlobClient;
  private final ProductMapper productMapper;
  private final String collectionName;
  private final ObjectMapper objectMapper;

  @ConfigProperty(name = "product-cdc.blob-storage.filepath-product") String productsFilePath;

  public ProductCdcService(ReactiveMongoClient mongoClient,
                           @ConfigProperty(name = "quarkus.mongodb.database") String mongodbDatabase,
                           @ConfigProperty(name = "quarkus.mongodb.collection") String collectionName,
                           @ConfigProperty(name = "product-cdc.mongodb.watch.enabled") Boolean cdcEnable,
                           TelemetryClient telemetryClient,
                           TableClient tableClient,
                           ProductService productService,
                           AzureBlobClient azureBlobClient,
                           ProductMapper productMapper, ObjectMapper objectMapper) {
    this.mongoClient = mongoClient;
    this.mongodbDatabase = mongodbDatabase;
    this.collectionName = collectionName;
    this.telemetryClient = telemetryClient;
    this.tableClient = tableClient;
    this.productService = productService;
    this.azureBlobClient = azureBlobClient;
    this.productMapper = productMapper;
    this.objectMapper = objectMapper;
    telemetryClient.getContext().getOperation().setName(ProductConstant.OPERATION_NAME);
    if (cdcEnable) {
      initOrderStream();
    }

  }

  private void initOrderStream() {
    log.info("Starting initOrderStream ... ");

    //Retrieve last resumeToken for watching collection at specific operation
    String resumeToken = null;

    if (!ConfigUtils.getProfiles().contains("test")) {
      try {
        TableEntity cdcStartAtEntity = tableClient.getEntity(ProductConstant.CDC_START_AT_PARTITION_KEY, ProductConstant.CDC_START_AT_ROW_KEY);
        if (Objects.nonNull(cdcStartAtEntity)){
          resumeToken = (String) cdcStartAtEntity.getProperty(ProductConstant.CDC_START_AT_PROPERTY);
        }
        log.info("Table StartAt: {}", resumeToken);
      } catch (TableServiceException e) {
        if (e.getResponse() != null && e.getResponse().getStatusCode() == 404) {
            log.warn("Table StartAt not found (404), starting from now. This is expected on first run.");
        } else {
            log.error("Error connecting to Table Storage. Code: {}, Message: {}", e.getResponse() != null ? e.getResponse().getStatusCode() : "N/A", e.getMessage(), e);
        }
      }
    }

    // Initialize watching collection
    ReactiveMongoCollection<Product> dataCollection = getCollection();
    ChangeStreamOptions options = new ChangeStreamOptions().fullDocument(FullDocument.UPDATE_LOOKUP);
    if (Objects.nonNull(resumeToken)) {
      options = options.resumeAfter(BsonDocument.parse(resumeToken));
    }

    Bson match = Aggregates.match(Filters.in("operationType", asList("update", "replace", "insert")));
    Bson project = Aggregates.project(fields(include("_id", "ns", "documentKey", "fullDocument")));
    List<Bson> pipeline = asList(match, project);

    Multi<ChangeStreamDocument<Product>> publisher = dataCollection.watch(pipeline, Product.class, options);
    publisher.subscribe().with(
      this::consumerEvent,
      failure -> {
        log.error("Error during subscribe collection, exception: {} , message: {}", failure.toString(), failure.getMessage());
        constructMapAndTrackEvent(failure.getClass().toString(), "FALSE", ProductConstant.PRODUCT_FAILURE_METRICS);
        Quarkus.asyncExit();
      });

    log.info("Completed initOrderStream ... ");
  }

  private ReactiveMongoCollection<Product> getCollection() {
    return mongoClient
      .getDatabase(mongodbDatabase)
      .getCollection(collectionName, Product.class);
  }

  protected void consumerEvent(ChangeStreamDocument<Product> document) {
    assert document.getFullDocument() != null;
    assert document.getDocumentKey() != null;

    log.info("Starting consumerProductEvent ... ");
    log.info("Product CDC update products having id {}", document.getFullDocument().getId());

    invokeCreationDocument(document.getFullDocument())
      .subscribe().with(
        result -> {
          log.info("Product CDC update products having id: {} successfull", document.getDocumentKey().toJson());
          updateLastResumeToken(document.getResumeToken());
          constructMapAndTrackEvent(document.getDocumentKey().toJson(), "TRUE", ProductConstant.PRODUCT_SUCCESS_METRICS);
        },
        failure -> {
          log.error("Product CDC update products having id: {} , message: {}", document.getDocumentKey().toJson(), failure.getMessage());
          constructMapAndTrackEvent(document.getDocumentKey().toJson(), "FALSE", ProductConstant.PRODUCT_FAILURE_METRICS);
        });
    log.info("End consumerProductEvent ... ");
  }

  public Uni<Object> invokeCreationDocument(Product product) {
    return Uni.createFrom().item(productService.getProducts(false, true))
      .onItem().transform(products -> {
        List<it.pagopa.selfcare.product.entity.Product> updateProducts = new ArrayList<>(products.stream().filter(p -> !p.getId().equals(product.getProductId())).toList());
        updateProducts.add(productMapper.toResource(product));
        return updateProducts;
      })
      .onItem().transformToUni(products -> Uni.createFrom().item(azureBlobClient.uploadFilePath(productsFilePath, convertListToJsonBytes(products))));
  }

  private void updateLastResumeToken(BsonDocument resumeToken) {
    // Table CdCStartAt will be updated with the last resume token
    Map<String, Object> properties = new HashMap<>();
    properties.put(ProductConstant.CDC_START_AT_PROPERTY, resumeToken.toJson());

    TableEntity tableEntity = new TableEntity(ProductConstant.CDC_START_AT_PARTITION_KEY, ProductConstant.CDC_START_AT_ROW_KEY)
      .setProperties(properties);
    tableClient.upsertEntity(tableEntity);

  }

  private void constructMapAndTrackEvent(String documentKey, String success, String... metrics) {
    Map<String, String> propertiesMap = new HashMap<>();
    propertiesMap.put("documentKey", documentKey);
    propertiesMap.put("success", success);

    Map<String, Double> metricsMap = new HashMap<>();
    Arrays.stream(metrics).forEach(metricName -> metricsMap.put(metricName, 1D));
    telemetryClient.trackEvent(ProductConstant.PRODUCT_CDC, propertiesMap, metricsMap);
  }

  public static Map<String, String> mapPropsForTrackEvent(TrackEventInput trackEventInput) {
    Map<String, String> propertiesMap = new HashMap<>();
    Optional.ofNullable(trackEventInput.getDocumentKey()).ifPresent(value -> propertiesMap.put("documentKey", value));
    Optional.ofNullable(trackEventInput.getUserId()).ifPresent(value -> propertiesMap.put("userId", value));
    Optional.ofNullable(trackEventInput.getProductId()).ifPresent(value -> propertiesMap.put("productId", value));
    Optional.ofNullable(trackEventInput.getInstitutionId()).ifPresent(value -> propertiesMap.put("institutionId", value));
    Optional.ofNullable(trackEventInput.getProductRole()).ifPresent(value -> propertiesMap.put("productRole", value));
    Optional.ofNullable(trackEventInput.getGroupMembers()).ifPresent(value -> propertiesMap.put("groupMembers", String.join(",", value)));
    Optional.ofNullable(trackEventInput.getException()).ifPresent(value -> propertiesMap.put("exec", value));
    return propertiesMap;
  }

  public byte[] convertListToJsonBytes(List<it.pagopa.selfcare.product.entity.Product> products) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(products);
    } catch (Exception e) {
      throw new RuntimeException("Error during JSON serialization", e);
    }
  }
}
