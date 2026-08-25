package it.pagopa.selfcare.commons.connector.rest.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

class PageJacksonModuleTest {

    private final ObjectMapper objectMapper;

    public PageJacksonModuleTest() {
        this.objectMapper = new ObjectMapper().registerModule(new PageJacksonModule());
    }

    @Test
    void test() throws JsonProcessingException {
        String src = "{\"content\":[\"test\"],\"totalElements\":1,\"number\":0,\"size\":1}";
        Page<String> read = objectMapper.readValue(src, new TypeReference<>() {
        });
        assertNotNull(read);
        assertNotNull(read.getContent());
        assertEquals(1, read.getContent().size());
        assertEquals("test", read.iterator().next());
        assertEquals(0, read.getNumber());
        assertEquals(1, read.getTotalElements());
        assertEquals(1, read.getTotalPages());
        assertEquals(1, read.getSize());
        assertEquals(1, read.getNumberOfElements());
        assertTrue(read.hasContent());
        assertNotNull(read.getSort());
        assertTrue(read.isFirst());
        assertTrue(read.isLast());
        assertFalse(read.hasNext());
        assertFalse(read.hasPrevious());
        assertNotNull(read.nextPageable());
        assertNotNull(read.previousPageable());
        assertEquals("pippo", read.map(s -> "pippo").iterator().next());
    }

}