package it.pagopa.selfcare.onboarding.mapper;

import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.UserDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PnPGUserMapperTest {

    @ParameterizedTest
    @ValueSource(classes = {UserDto.class, UserDataValidationDto.class})
    void toUser_null(Class<?> clazz) {
        final UserMapper userMapper = new UserMapperImpl();

        User resource;
        if (UserDto.class.isAssignableFrom(clazz)) {
            resource = userMapper.toUser((UserDto) null);
        } else {
            resource = userMapper.toUser((UserDataValidationDto) null);
        }

        assertNull(resource);
    }
}
