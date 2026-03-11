package it.pagopa.selfcare.user_group.model;

import it.pagopa.selfcare.user_group.model.mapper.UserGroupMapper;
import it.pagopa.selfcare.user_group.model.mapper.UserGroupMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserGroupMapperTest {

    @Test
    void toSortWithNullTest() {
        final UserGroupMapper mapper = new UserGroupMapperImpl();
        final Sort sort = mapper.toSort(null);
        assertTrue(sort.isUnsorted());
    }

    @Test
    void toSortWithBlankStringTest() {
        final UserGroupMapper mapper = new UserGroupMapperImpl();
        final Sort sort = mapper.toSort("   ");
        assertTrue(sort.isUnsorted());
    }

    @Test
    void toSortWithSingleParameterTest() {
        final UserGroupMapper mapper = new UserGroupMapperImpl();
        final Sort sort = mapper.toSort(" name ");
        assertEquals(new Sort.Order(Sort.Direction.ASC, "name"), sort.getOrderFor("name"));
    }

    @Test
    void toSortWithSingleParameterAndDirectionTest() {
        final UserGroupMapper mapper = new UserGroupMapperImpl();
        final Sort sort = mapper.toSort(" name , DESC");
        assertEquals(new Sort.Order(Sort.Direction.DESC, "name"), sort.getOrderFor("name"));
    }

    @Test
    void toSortWithMultipleParametersAndDirectionsTest() {
        final UserGroupMapper mapper = new UserGroupMapperImpl();
        final Sort sort = mapper.toSort(" name , ASC, description, productId, DESC, institutionId, parentInstitutionId");
        assertEquals(new Sort.Order(Sort.Direction.ASC, "name"), sort.getOrderFor("name"));
        assertEquals(new Sort.Order(Sort.Direction.ASC, "description"), sort.getOrderFor("description"));
        assertEquals(new Sort.Order(Sort.Direction.DESC, "productId"), sort.getOrderFor("productId"));
        assertEquals(new Sort.Order(Sort.Direction.ASC, "institutionId"), sort.getOrderFor("institutionId"));
        assertEquals(new Sort.Order(Sort.Direction.ASC, "parentInstitutionId"), sort.getOrderFor("parentInstitutionId"));
    }

}
