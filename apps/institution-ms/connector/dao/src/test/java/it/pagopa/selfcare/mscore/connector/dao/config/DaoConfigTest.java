package it.pagopa.selfcare.mscore.connector.dao.config;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@TestConfiguration
@Import(DaoConfig.class)
public class DaoConfigTest {
    @InjectMocks
    private DaoConfig daoConfig;

    @Mock
    private MongoDatabaseFactory mongoDatabaseFactory;


    @Test
    void testCustomConversions() {
        Assertions.assertNotNull(daoConfig.customConversions());
    }

}

