package it.pagopa.selfcare.commons.base.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collection;
import java.util.List;

import static it.pagopa.selfcare.commons.base.security.PartyRole.*;
import static it.pagopa.selfcare.commons.base.security.SelfCareAuthority.ADMIN;
import static org.junit.jupiter.api.Assertions.*;

class ProductGrantedAuthorityTest {

    @Test
    void ProductGrantedAuthority_nullRole() {
        // given
        PartyRole partyRole = null;
        Collection<String> productRoles = List.of("productRole");
        String productId = "productId";
        // when
        Executable executable = () -> new ProductGrantedAuthority(partyRole, productRoles, productId);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A Party Role is required", e.getMessage());
    }

    @Test
    void ProductGrantedAuthority_nullProductRole() {
        // given
        PartyRole partyRole = MANAGER;
        Collection<String> productRoles = null;
        String productId = "productId";
        // when
        Executable executable = () -> new ProductGrantedAuthority(partyRole, productRoles, productId);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("At least one Product granted authority textual representation is required", e.getMessage());
    }

    @Test
    void ProductGrantedAuthority_nullProductCode() {
        // given
        PartyRole partyRole = MANAGER;
        Collection<String> productRoles = List.of("productRole");
        String productId = "";
        // when
        Executable executable = () -> new ProductGrantedAuthority(partyRole, productRoles, productId);
        // then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A Product id is required", e.getMessage());
    }


    @Test
    void ProductGrantedAuthority() {
        // given
        PartyRole partyRole = MANAGER;
        Collection<String> productRoles = List.of("productRole");
        String productId = "productId";
        // when
        ProductGrantedAuthority authority = new ProductGrantedAuthority(partyRole, productRoles, productId);
        // then
        assertEquals(ADMIN.name(), authority.getAuthority());
        assertIterableEquals(productRoles, authority.getProductRoles());
        assertEquals(productId, authority.getProductId());
        ProductGrantedAuthority authority2 = new ProductGrantedAuthority(OPERATOR, List.of("role2"), productId);
        assertEquals(authority2, authority);
        assertEquals(authority2.hashCode(), authority.hashCode());
        ProductGrantedAuthority authority3 = new ProductGrantedAuthority(SUB_DELEGATE, productRoles, "productId2");
        assertNotEquals(authority3, authority);
        assertNotEquals(authority3.hashCode(), authority.hashCode());
    }


    @Test
    void mergeProductGrantedAuthority_differentPartyRole() {
        // given
        Collection<String> productRoles = List.of("productRole");
        String productId = "productId";
        ProductGrantedAuthority authority1 = new ProductGrantedAuthority(MANAGER, productRoles, productId);
        ProductGrantedAuthority authority2 = new ProductGrantedAuthority(OPERATOR, productRoles, productId);
        // when
        Executable executable = () -> ProductGrantedAuthority.MERGE.apply(authority1, authority2);
        // then
        assertThrows(IllegalStateException.class, executable);
    }


    @Test
    void mergeProductGrantedAuthority() {
        // given
        PartyRole partyRole = MANAGER;
        String productRole1 = "productRole1";
        String productRole2 = "productRole2";
        String productId = "productId";
        ProductGrantedAuthority authority1 = new ProductGrantedAuthority(partyRole, productRole1, productId);
        ProductGrantedAuthority authority2 = new ProductGrantedAuthority(partyRole, productRole2, productId);
        // when
        ProductGrantedAuthority merged = ProductGrantedAuthority.MERGE.apply(authority1, authority2);
        // then
        assertEquals(productId, merged.getProductId());
        assertEquals(ADMIN.toString(), merged.getAuthority());
        assertIterableEquals(List.of(productRole1, productRole2), merged.getProductRoles());
    }

}