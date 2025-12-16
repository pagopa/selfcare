package it.pagopa.selfcare.product.repository;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.product.model.ContractTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
public class ContractTemplateRepositoryTest {

    @Inject
    private ContractTemplateRepository contractTemplateRepository;

    @BeforeEach
    void setup() {
        contractTemplateRepository.deleteAll().await().indefinitely();
        contractTemplateRepository.persist(List.of(
                ContractTemplate.builder().productId("prod-1").name("testname1").version("0.0.1")
                        .createdAt(Instant.parse("2010-12-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-1").name("testname1").version("0.1.1")
                        .createdAt(Instant.parse("2007-12-20T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-2").name("testname2").version("0.0.1")
                        .createdAt(Instant.parse("2009-12-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-1").name("testname2").version("0.0.1")
                        .createdAt(Instant.parse("2011-12-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-2").name("testname1").version("0.0.1")
                        .createdAt(Instant.parse("2007-12-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-1").name("testname3").version("0.0.1")
                        .createdAt(Instant.parse("2008-12-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-2").name("testname3").version("1.0.0")
                        .createdAt(Instant.parse("2007-07-03T10:15:30.00Z")).build(),
                ContractTemplate.builder().productId("prod-1").name("testname3").version("1.2.1")
                        .createdAt(Instant.parse("2007-08-03T10:15:30.00Z")).build()
        )).await().indefinitely();
    }

    @Test
    void countWithFiltersTest() {
        Assertions.assertEquals(5L, contractTemplateRepository.countWithFilters("prod-1", null, null).await().indefinitely());
        Assertions.assertEquals(3L, contractTemplateRepository.countWithFilters("prod-2", null, null).await().indefinitely());
        Assertions.assertEquals(0L, contractTemplateRepository.countWithFilters("prod-3", null, null).await().indefinitely());

        Assertions.assertEquals(2L, contractTemplateRepository.countWithFilters("prod-1", "testname1", null).await().indefinitely());
        Assertions.assertEquals(1L, contractTemplateRepository.countWithFilters("prod-2", "testname2", null).await().indefinitely());
        Assertions.assertEquals(0L, contractTemplateRepository.countWithFilters("prod-2", "testname5", null).await().indefinitely());

        Assertions.assertEquals(1L, contractTemplateRepository.countWithFilters("prod-1", "testname1", "0.0.1").await().indefinitely());
        Assertions.assertEquals(1L, contractTemplateRepository.countWithFilters("prod-2", "testname2", "0.0.1").await().indefinitely());
        Assertions.assertEquals(0L, contractTemplateRepository.countWithFilters("prod-2", "testname2", "2.0.0").await().indefinitely());

        Assertions.assertEquals(2L, contractTemplateRepository.countWithFilters(null, "testname2", null).await().indefinitely());
        Assertions.assertEquals(1L, contractTemplateRepository.countWithFilters("prod-1", "testname2", null).await().indefinitely());
        Assertions.assertEquals(3L, contractTemplateRepository.countWithFilters("prod-1", null, "0.0.1").await().indefinitely());
    }

    @Test
    void listWithFiltersTest() {
        final List<ContractTemplate> result0 = contractTemplateRepository.listWithFilters(null, null, null).await().indefinitely();
        Assertions.assertEquals(8, result0.size());

        final List<ContractTemplate> result1 = contractTemplateRepository.listWithFilters("prod-1", null, null).await().indefinitely();
        Assertions.assertEquals(5, result1.size());
        Assertions.assertEquals("testname2", result1.get(0).getName());
        Assertions.assertEquals("0.0.1", result1.get(0).getVersion());
        Assertions.assertEquals("testname1", result1.get(1).getName());
        Assertions.assertEquals("0.0.1", result1.get(1).getVersion());
        Assertions.assertEquals("testname3", result1.get(2).getName());
        Assertions.assertEquals("0.0.1", result1.get(2).getVersion());
        Assertions.assertEquals("testname1", result1.get(3).getName());
        Assertions.assertEquals("0.1.1", result1.get(3).getVersion());
        Assertions.assertEquals("testname3", result1.get(4).getName());
        Assertions.assertEquals("1.2.1", result1.get(4).getVersion());

        final List<ContractTemplate> result2 = contractTemplateRepository.listWithFilters("prod-2", null, null).await().indefinitely();
        Assertions.assertEquals(3, result2.size());
        Assertions.assertEquals("testname2", result2.get(0).getName());
        Assertions.assertEquals("0.0.1", result2.get(0).getVersion());
        Assertions.assertEquals("testname1", result2.get(1).getName());
        Assertions.assertEquals("0.0.1", result2.get(1).getVersion());
        Assertions.assertEquals("testname3", result2.get(2).getName());
        Assertions.assertEquals("1.0.0", result2.get(2).getVersion());

        final List<ContractTemplate> result3 = contractTemplateRepository.listWithFilters("prod-1", "testname3", null).await().indefinitely();
        Assertions.assertEquals(2, result3.size());
        Assertions.assertEquals("testname3", result3.get(0).getName());
        Assertions.assertEquals("0.0.1", result3.get(0).getVersion());
        Assertions.assertEquals("testname3", result3.get(1).getName());
        Assertions.assertEquals("1.2.1", result3.get(1).getVersion());

        final List<ContractTemplate> result4 = contractTemplateRepository.listWithFilters("prod-1", "testname3", "1.2.1").await().indefinitely();
        Assertions.assertEquals(1, result4.size());
        Assertions.assertEquals("testname3", result4.get(0).getName());
        Assertions.assertEquals("1.2.1", result4.get(0).getVersion());

        final List<ContractTemplate> result5 = contractTemplateRepository.listWithFilters("prod-1", null, "1.2.1").await().indefinitely();
        Assertions.assertEquals(1, result5.size());
        Assertions.assertEquals("testname3", result5.get(0).getName());
        Assertions.assertEquals("1.2.1", result5.get(0).getVersion());
    }

}
