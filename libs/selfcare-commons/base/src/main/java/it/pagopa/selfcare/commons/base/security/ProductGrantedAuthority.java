package it.pagopa.selfcare.commons.base.security;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;

@ToString
@EqualsAndHashCode(of = "productId")
@Builder(access = AccessLevel.PRIVATE, toBuilder = true)
public class ProductGrantedAuthority implements GrantedAuthority {

    @Getter
    private final PartyRole partyRole;
    @Getter
    private final Collection<String> productRoles;
    @Getter
    private final String productId;


    public ProductGrantedAuthority(PartyRole partyRole, String productRole, String productId) {
        this(partyRole, List.of(productRole), productId);
    }


    public ProductGrantedAuthority(PartyRole partyRole, Collection<String> productRoles, String productId) {
        Assert.notNull(partyRole, "A Party Role is required");
        Assert.notEmpty(productRoles, "At least one Product granted authority textual representation is required");
        Assert.hasText(productId, "A Product id is required");
        this.partyRole = partyRole;
        this.productRoles = Collections.unmodifiableCollection(productRoles);
        this.productId = productId;
    }


    @Override
    public String getAuthority() {
        return partyRole.getSelfCareAuthority().name();
    }


    public static final BinaryOperator<ProductGrantedAuthority> MERGE = (o1, o2) -> {
        Assert.state(o1.getAuthority().equals(o2.getAuthority()), "Multirole on product is allowed only for same PartyRole");
        ArrayList<String> roles = new ArrayList<>(o1.getProductRoles());
        roles.addAll(o2.getProductRoles());
        return o1.toBuilder().productRoles(roles).build();
    };

}
