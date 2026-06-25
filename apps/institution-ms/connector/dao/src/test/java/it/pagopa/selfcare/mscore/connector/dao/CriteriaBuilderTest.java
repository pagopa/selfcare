package it.pagopa.selfcare.mscore.connector.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {CriteriaBuilder.class})
@ExtendWith(MockitoExtension.class)
class CriteriaBuilderTest {

    @Test
    void testInIfNotEmpty() {
        Criteria criteria = CriteriaBuilder.builder()
                .inIfNotEmpty("colors", List.of("red", "green", "blue"))
                .inIfNotEmpty("sizes", List.of("small", "medium", "large"))
                .build();

        org.bson.conversions.Bson bson = criteria.getCriteriaObject();
        assertEquals("Document{{colors=Document{{$in=[red, green, blue]}}, sizes=Document{{$in=[small, medium, large]}}}}", bson.toString());
    }

    @Test
    void testIsIfNotNull() {
        Criteria criteria = CriteriaBuilder.builder()
                .isIfNotNull("quantity", 10)
                .isIfNotNull("price", 19.99)
                .build();

        org.bson.conversions.Bson bson = criteria.getCriteriaObject();

        assertEquals("Document{{quantity=10, price=19.99}}", bson.toString());
    }
}
