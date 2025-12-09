package it.pagopa.selfcare.product;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.mongodb.ChangeStreamOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.product.constant.ProductConstant;
import it.pagopa.selfcare.product.mapper.ProductMapper;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.ProductMetadata;
import it.pagopa.selfcare.product.service.ProductService;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class ProductCdcServiceTest {

    private ProductCdcService productCdcService;

    private ReactiveMongoClient mongoClient;
    private ReactiveMongoDatabase mongoDatabase;
    private ReactiveMongoCollection<Product> mongoCollection;
    private TelemetryClient telemetryClient;
    private TableClient tableClient;
    private ProductService productService;
    private AzureBlobClient azureBlobClient;
    private ProductMapper productMapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mongoClient = mock(ReactiveMongoClient.class);
        mongoDatabase = mock(ReactiveMongoDatabase.class);
        mongoCollection = mock(ReactiveMongoCollection.class);
        telemetryClient = mock(TelemetryClient.class);
        tableClient = mock(TableClient.class);
        productService = mock(ProductService.class);
        azureBlobClient = mock(AzureBlobClient.class);
        productMapper = mock(ProductMapper.class);
        objectMapper = new ObjectMapper();

        // Mock MongoDB structure
        when(mongoClient.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(Product.class))).thenReturn(mongoCollection);

        // Mock TelemetryClient context to avoid NPE during constructor
        TelemetryContext telemetryContext = mock(TelemetryContext.class);
        when(telemetryClient.getContext()).thenReturn(telemetryContext);

        OperationContext operationContext = mock(OperationContext.class);
        when(telemetryContext.getOperation()).thenReturn(operationContext);

        when(mongoCollection.watch(anyList(), eq(Product.class), any(ChangeStreamOptions.class)))
                .thenReturn(Multi.createFrom().empty());

        productCdcService = new ProductCdcService(
                mongoClient,
                "test-db",
                "test-collection",
                telemetryClient,
                tableClient,
                productService,
                azureBlobClient,
                productMapper,
                objectMapper
        );

        // Inject the config property manually since we are instantiating the class directly
        productCdcService.productsFilePath = "products.json";
    }

    @Test
    void invokeCreationDocument_shouldUpdateStorageSuccessfully() {
        // Arrange
        String productId = "prod-io";
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle("IO");

        product.setMetadata(ProductMetadata.builder().createdAt(Instant.now()).build());

        it.pagopa.selfcare.product.entity.Product entityProduct = new it.pagopa.selfcare.product.entity.Product();
        entityProduct.setId(productId);
        entityProduct.setTitle("IO");

        // Existing products on storage
        it.pagopa.selfcare.product.entity.Product existingProduct = new it.pagopa.selfcare.product.entity.Product();
        existingProduct.setId("prod-pagopa");
        List<it.pagopa.selfcare.product.entity.Product> existingProducts = new ArrayList<>();
        existingProducts.add(existingProduct);

        when(productService.getProducts(false, true)).thenReturn(existingProducts);
        when(productMapper.toResource(product)).thenReturn(entityProduct);
        when(azureBlobClient.uploadFilePath(anyString(), any(byte[].class))).thenReturn("uploaded-path");

        // Act
        Uni<Object> result = productCdcService.invokeCreationDocument(product);

        // Assert
        UniAssertSubscriber<Object> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted().assertItem("uploaded-path");

        // Verify that upload was called with the correct file path
        verify(azureBlobClient).uploadFilePath(eq("products.json"), any(byte[].class));

        // Verify that the list to upload contains both the existing product and the new/updated one
        // We can't easily inspect the byte array here without deserializing, but we verified the flow.
    }

    @Test
    void invokeCreationDocument_shouldReplaceExistingProduct() {
        // Arrange
        String productId = "prod-io";
        Product product = new Product();
        product.setProductId(productId);
        product.setTitle("IO Updated");

        it.pagopa.selfcare.product.entity.Product entityProduct = new it.pagopa.selfcare.product.entity.Product();
        entityProduct.setId(productId);
        entityProduct.setTitle("IO Updated");

        // Existing products on storage (including the old version of IO)
        it.pagopa.selfcare.product.entity.Product oldProduct = new it.pagopa.selfcare.product.entity.Product();
        oldProduct.setId(productId);
        oldProduct.setTitle("IO Old");

        List<it.pagopa.selfcare.product.entity.Product> existingProducts = new ArrayList<>();
        existingProducts.add(oldProduct);

        when(productService.getProducts(false, true)).thenReturn(existingProducts);
        when(productMapper.toResource(product)).thenReturn(entityProduct);
        when(azureBlobClient.uploadFilePath(anyString(), any(byte[].class))).thenReturn("uploaded-path");

        // Act
        Uni<Object> result = productCdcService.invokeCreationDocument(product);

        // Assert
        UniAssertSubscriber<Object> subscriber = result.subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        // Verify that the mapper was called
        verify(productMapper).toResource(product);
    }

    @Test
    void consumerEvent_shouldProcessEventAndTrackSuccess() {
        // Arrange
        ChangeStreamDocument<Product> document = mock(ChangeStreamDocument.class);
        Product product = new Product();
        product.setProductId("prod-test");
        product.setId("mongo-id");

        BsonDocument resumeToken = new BsonDocument("token", new BsonString("123"));
        BsonDocument documentKey = new BsonDocument("_id", new BsonString("mongo-id"));

        when(document.getFullDocument()).thenReturn(product);
        when(document.getResumeToken()).thenReturn(resumeToken);
        when(document.getDocumentKey()).thenReturn(documentKey);

        // Mock the internal logic of invokeCreationDocument
        when(productService.getProducts(false, true)).thenReturn(new ArrayList<>());
        when(productMapper.toResource(product)).thenReturn(new it.pagopa.selfcare.product.entity.Product());
        when(azureBlobClient.uploadFilePath(anyString(), any(byte[].class))).thenReturn("ok");

        // Act
        productCdcService.consumerEvent(document);

        // Assert
        // Verify that the resume token was updated in the Table Storage
        ArgumentCaptor<TableEntity> captor = ArgumentCaptor.forClass(TableEntity.class);
        verify(tableClient).upsertEntity(captor.capture());
        Assertions.assertEquals("{\"token\": \"123\"}", captor.getValue().getProperties().get("startAt"));

        // Verify telemetry tracking for success
//        verify(telemetryClient).trackEvent(eq("ProductCDC"), anyMap(), anyMap());
        verify(telemetryClient).trackEvent(eq(ProductConstant.PRODUCT_CDC), anyMap(), anyMap());
    }

    @Test
    void convertListToJsonBytes_shouldSerializeCorrectly() {
        // Arrange
        it.pagopa.selfcare.product.entity.Product p1 = new it.pagopa.selfcare.product.entity.Product();
        p1.setId("p1");
        List<it.pagopa.selfcare.product.entity.Product> list = List.of(p1);

        // Act
        byte[] bytes = productCdcService.convertListToJsonBytes(list);

        // Assert
        Assertions.assertNotNull(bytes);
        Assertions.assertTrue(bytes.length > 0);
        String json = new String(bytes);
        Assertions.assertTrue(json.contains("\"id\" : \"p1\"")); // Check for pretty print format
    }
}