package it.pagopa.selfcare.iam.cucumber;

import com.mongodb.client.MongoDatabase;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.restassured.RestAssured;
import it.pagopa.selfcare.iam.cucumber.config.IntegrationProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;

import java.io.IOException;

@Slf4j
@CucumberOptions(
  features = "src/test/resources/features",
  glue = {"it.pagopa.selfcare.iam.cucumber.steps"},
  plugin = {
    "pretty",
    "html:target/cucumber-report/cucumber.html",
    "json:target/cucumber-report/cucumber.json"
  })
public class CucumberSuiteTest extends CucumberQuarkusTest {

  static MongoDatabase mongoDatabase;
    private static ComposeContainer composeContainer;

    public static void main(String[] args) {
        runMain(CucumberSuiteTest.class, args);
    }

    @BeforeAll
    static void setup() throws IOException {
//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        try (InputStream inputStream = classLoader.getResourceAsStream("key/public-key.pub")) {
//            if (inputStream == null) {
//                throw new IOException("Public key file not found in classpath");
//            }
//            String publicKey = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
//            System.setProperty("JWT-PUBLIC-KEY", publicKey);
//        }

        // By default, quarkus starts the ms on port 8081
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;

      log.info("Starting test containers...");
//        composeContainer = new ComposeContainer(new File("docker-compose.yml"))
//          .withLocalCompose(true).withPull(true)
////                .withExposedService("userms", 8080)
//                .waitingFor("mongodb", Wait.forListeningPort())
////                .waitingFor("userms", Wait.forHttp("/q/health/ready").forPort(8080).forStatusCode(200))
////                .waitingFor("institutionms", Wait.forLogMessage(".*Started SelfCareCoreApplication.*\\n", 1))
////                .waitingFor("externalms", Wait.forLogMessage(".*Started SelfCareExternalAPIApplication.*\\n", 1))
////                .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1))
//                .withStartupTimeout(Duration.ofMinutes(5));
//
//        composeContainer.start();
//        Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));
        log.info("Test containers started successfully");
        log.info("\nLANGUAGE: {}\nCOUNTRY: {}\nTIMEZONE: {}\n", System.getProperty("user.language"), System.getProperty("user.country"), System.getProperty("user.timezone"));
    }

   private static void initDb() {
     mongoDatabase = IntegrationProfile.getMongoClientConnection();
    }


  @AfterAll
    static void tearDown() {
        log.info("Cucumber tests are finished.");
    }

}
