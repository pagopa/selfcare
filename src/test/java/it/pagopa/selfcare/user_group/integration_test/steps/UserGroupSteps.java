package it.pagopa.selfcare.user_group.integration_test.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import it.pagopa.selfcare.user_group.model.TestProperties;
import it.pagopa.selfcare.user_group.model.UserGroupEntityPageable;
import it.pagopa.selfcare.user_group.dao.UserGroupRepository;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
    protected String token = readDataPopulation().getToken();

    public TestProperties readDataPopulation() {
        TestProperties testProperties = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            testProperties = objectMapper.readValue(new File("src/test/resources/dataPopulation/data.json"), new TypeReference<>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testProperties;
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
