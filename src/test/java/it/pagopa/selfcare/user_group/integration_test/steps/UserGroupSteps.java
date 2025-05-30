package it.pagopa.selfcare.user_group.integration_test.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.cucumber.utils.model.JwtData;
import it.pagopa.selfcare.cucumber.utils.model.TestData;
import it.pagopa.selfcare.user_group.dao.UserGroupRepository;
import it.pagopa.selfcare.user_group.integration_test.KeyGenerator;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupEntityPageable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class UserGroupSteps {

    @Autowired
    protected UserGroupRepository userGroupRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String userGroupId;
    protected UUID userGroupMemberId;
    protected UserGroupEntity userGroupDetails;
    protected UserGroupEntity userGroupEntityResponse;
    protected UserGroupEntity updatedUserGroupEntity;
    protected UserGroupEntityPageable userGroupEntityResponsePage;
    protected Pageable pageable;
    protected UserGroupEntity userGroupEntityFilter;
    protected int status;
    protected String errorMessage;
    protected List<String> userGroupsIds = List.of("6759f8df78b6af202b222d29", "6759f8df78b6af202b222d2a", "6759f8df78b6af202b222d2b");
    protected String token;

    protected void login(String user, String pass) {
        TestData testData = readDataPopulation();
        JwtData jwtData = testData.getJwtData().stream()
                .filter(data -> data.getUsername().equals(user) && data.getPassword().equals(pass))
                .findFirst()
                .orElse(null);
        token = generateToken(jwtData);
    }

    public String generateToken(JwtData jwtData) {
        if (Objects.nonNull(jwtData)) {
            try {
                File file = new File("src/test/resources/key/private-key.pem");
                Algorithm alg = Algorithm.RSA256(KeyGenerator.getPrivateKey(new String(Files.readAllBytes(file.toPath()))));
                String jwt = JWT.create()
                        .withHeader(jwtData.getJwtHeader())
                        .withPayload(jwtData.getJwtPayload())
                        .withIssuedAt(Instant.now())
                        .withExpiresAt(Instant.now().plusSeconds(3600))
                        .sign(alg);
                log.info("generated token jwt");
                return jwt;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public TestData readDataPopulation() {
        TestData testData = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            testData = objectMapper.readValue(new File("src/test/resources/dataPopulation/data.json"), new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testData;
    }

    public void verifyErrorMessage(String expectedErrorMessage) {
        String[] errorMessageArray = expectedErrorMessage.split(",");
        Arrays.stream(errorMessageArray).forEach(s -> Assertions.assertTrue(errorMessage.contains(s)));
    }

    public void verifyResponseStatus(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, status);
    }

    public void verifyNotNull(Object... objects) {
        Arrays.stream(objects).forEach(Assertions::assertNotNull);
    }

    public void verifyNull(Object... objects) {
        Arrays.stream(objects).forEach(Assertions::assertNull);
    }

    public void initializeCollection() throws IOException {
        List<UserGroupEntity> groupsToInsert = objectMapper.readValue(new File("src/test/resources/dataPopulation/groupEntities.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, UserGroupEntity.class));
        userGroupRepository.insert(groupsToInsert);
    }

}
