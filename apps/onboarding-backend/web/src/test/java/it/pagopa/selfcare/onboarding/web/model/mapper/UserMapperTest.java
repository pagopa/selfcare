package it.pagopa.selfcare.onboarding.web.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.UserInfo;
import it.pagopa.selfcare.onboarding.web.model.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.web.model.UserDto;
import it.pagopa.selfcare.onboarding.web.model.UserResource;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserMapperTest {

    private UserResourceMapper userMapper = new UserResourceMapperImpl();

    @ParameterizedTest
    @ValueSource(classes = {
            UserDto.class,
            UserDataValidationDto.class
    })
    void toUser(Class<?> clazz) throws Exception {
        //given
        final Object model = TestUtils.mockInstance(clazz.getDeclaredConstructor().newInstance());
        //when
        User resource;
        if (UserDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDto) model);
        } else if (UserDataValidationDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDataValidationDto) model);
        } else {
            throw new IllegalArgumentException();
        }
        //then
        assertNotNull(resource);
        TestUtils.reflectionEqualsByName(resource, model);
    }


    @ParameterizedTest
    @ValueSource(classes = {
            UserDto.class,
            UserDataValidationDto.class
    })
    void toUser_null(Class<?> clazz) {
        //given
        //when
        User resource;
        if (UserDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDto) null);
        } else if (UserDataValidationDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDataValidationDto) null);
        } else {
            throw new IllegalArgumentException();
        }
        //then
        assertNull(resource);
    }


    @Test
    void toResource_userResource() {
        //given
        UserInfo model = TestUtils.mockInstance(new UserInfo(), "setId", "setInstitutionId");
        model.setId(UUID.randomUUID().toString());
        model.setInstitutionId(UUID.randomUUID().toString());
        //when
        UserResource resource = userMapper.toResource(model);
        //then
        assertNotNull(resource);
        TestUtils.reflectionEqualsByName(resource, model, "id", "institutionId");
        assertEquals(model.getId(), resource.getId().toString());
        assertEquals(model.getInstitutionId(), resource.getInstitutionId().toString());
    }


    @Test
    void toResource_nullUserResource() {
        //given
        //when
        UserResource resource = userMapper.toResource(null);
        //then
        assertNull(resource);
    }

}