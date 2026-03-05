package it.pagopa.selfcare.onboarding.steps;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.entity.DataProtectionOfficer;
import it.pagopa.selfcare.onboarding.entity.GPUData;
import it.pagopa.selfcare.onboarding.entity.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
@QuarkusTest
public class IntegrationOperationUtils {

  @Inject OnboardingRepository onboardingRepository;

  @Inject TokenRepository tokenRepository;

  public <T> void persistIntoMongo(T input) {
    if (input instanceof Token) {
      tokenRepository.persist((Token) input);
    } else {
      onboardingRepository.persist((Onboarding) input);
    }
  }

  public Onboarding findIntoMongoOnboarding(String id) {
    return onboardingRepository.findById(id);
  }

  public Token findIntoMongoToken(String id) {
    return tokenRepository.findById(id);
  }

  public void persistInstitution(MongoDatabase mongoDatabase, Institution institution) {
    MongoCollection<Document> collection = mongoDatabase.getCollection("Institution");

    collection.insertOne(insertInstitution(institution));
  }

  public Document insertInstitution(Institution institution) {
    Document document = new Document();

    try {
      String documentId = institution.getId();
      if (documentId == null || documentId.trim().isEmpty()) {
        documentId = UUID.randomUUID().toString();
      }
      document.append("_id", documentId);
      document.append(
          "institutionType",
          institution.getInstitutionType() != null
              ? institution.getInstitutionType().toString()
              : null);
      document.append("taxCode", institution.getTaxCode());
      document.append("subunitCode", institution.getSubunitCode());
      document.append(
          "subunitType",
          institution.getSubunitType() != null ? institution.getSubunitType().toString() : null);

      document.append(
          "origin", institution.getOrigin() != null ? institution.getOrigin().toString() : null);
      document.append("originId", institution.getOriginId());

      document.append("city", institution.getCity());
      document.append("country", institution.getCountry());
      document.append("county", institution.getCounty());
      document.append("description", institution.getDescription());
      document.append("digitalAddress", institution.getDigitalAddress());
      document.append("address", institution.getAddress());
      document.append("zipCode", institution.getZipCode());

      if (institution.getGeographicTaxonomies() != null) {
        List<Document> taxonomyDocs = new ArrayList<>();
        for (GeographicTaxonomy gt : institution.getGeographicTaxonomies()) {
          Document taxDoc =
              new Document().append("code", gt.getCode()).append("desc", gt.getDesc());
          taxonomyDocs.add(taxDoc);
        }
        document.append("geographicTaxonomies", taxonomyDocs);
      }

      document.append("rea", institution.getRea());
      document.append("shareCapital", institution.getShareCapital());
      document.append("businessRegisterPlace", institution.getBusinessRegisterPlace());

      document.append("supportEmail", institution.getSupportEmail());
      document.append("supportPhone", institution.getSupportPhone());

      document.append("imported", institution.isImported());

      if (institution.getPaymentServiceProvider() != null) {
        PaymentServiceProvider psp = institution.getPaymentServiceProvider();
        Document pspDoc =
            new Document()
                .append("abiCode", psp.getAbiCode())
                .append("businessRegisterNumber", psp.getBusinessRegisterNumber())
                .append("legalRegisterName", psp.getLegalRegisterName())
                .append("legalRegisterNumber", psp.getLegalRegisterNumber())
                .append("vatNumberGroup", psp.isVatNumberGroup());
        document.append("paymentServiceProvider", pspDoc);
      }

      if (institution.getDataProtectionOfficer() != null) {
        DataProtectionOfficer dpo = institution.getDataProtectionOfficer();
        Document dpoDoc =
            new Document()
                .append("address", dpo.getAddress())
                .append("email", dpo.getEmail())
                .append("pec", dpo.getPec());
        document.append("dataProtectionOfficer", dpoDoc);
      }

      if (institution.getGpuData() != null) {
        GPUData gpu = institution.getGpuData();
        Document gpuDoc =
            new Document()
                .append("manager", gpu.isManager())
                .append("managerAuthorized", gpu.isManagerAuthorized());
        document.append("gpuData", gpuDoc);
      }

      document.append("parentDescription", institution.getParentDescription());

    } catch (Exception e) {
      log.error("Error: ", e);
    }

    return document;
  }
}
