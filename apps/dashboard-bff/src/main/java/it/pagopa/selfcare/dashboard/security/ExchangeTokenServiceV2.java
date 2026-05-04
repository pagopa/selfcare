package it.pagopa.selfcare.dashboard.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.*;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.commons.base.security.ProductGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.security.JwtService;
import it.pagopa.selfcare.dashboard.client.IamExternalRestClient;
import it.pagopa.selfcare.dashboard.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.config.ExchangeTokenProperties;
import it.pagopa.selfcare.dashboard.exception.InvalidRequestException;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.model.groups.UserGroup;
import it.pagopa.selfcare.dashboard.model.institution.InstitutionBackofficeAdmin;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import it.pagopa.selfcare.dashboard.model.institution.RootParent;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapper;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.dashboard.model.user.OnboardedProduct;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.UserInstitution;
import it.pagopa.selfcare.dashboard.service.InstitutionService;
import it.pagopa.selfcare.dashboard.service.UserGroupV2Service;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissions;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.owasp.encoder.Encode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Service
public class ExchangeTokenServiceV2 {

    private static final String PRIVATE_KEY_HEADER_TEMPLATE = "-----BEGIN %s-----";
    private static final String PRIVATE_KEY_FOOTER_TEMPLATE = "-----END %s-----";
    private static final String INSTITUTION_REQUIRED_MESSAGE = "Institution info is required";
    private static final String AUTHENTICATION_REQUIRED_MESSAGE = "Authentication is required";
    private static final String TOKEN_CLAIMS_REQUIRED_MESSAGE = "Session token claims is required";
    private static final String ID = "ID";
    private final String billingUrl;
    private final String billingAudience;
    private final PrivateKey jwtSigningKey;
    private final JwtService jwtService;
    private final Duration duration;
    private final String kid;
    private final InstitutionService institutionService;
    private final UserGroupV2Service groupService;
    public final UserV2Service userService;
    private final ProductService productService;
    private final UserInstitutionApiRestClient userInstitutionApiRestClient;
    private final IamExternalRestClient iamExternalRestClient;
    private final String issuer;

    private final InstitutionResourceMapper institutionResourceMapper;
    private final InstitutionMapper institutionMapper;
    private final ProductMapper productMapper;

    public ExchangeTokenServiceV2(JwtService jwtService,
                                  InstitutionService institutionService,
                                  UserGroupV2Service groupService,
                                  ExchangeTokenProperties properties,
                                  UserV2Service userService, ProductService productService, UserInstitutionApiRestClient userInstitutionApiRestClient, IamExternalRestClient iamExternalRestClient,
                                  InstitutionResourceMapper institutionResourceMapper, InstitutionMapper institutionMapper, ProductMapper productMapper)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        this.billingUrl = properties.getBillingUrl();
        this.billingAudience = properties.getBillingAudience();
        this.jwtService = jwtService;
        this.institutionService = institutionService;
        this.groupService = groupService;
        this.jwtSigningKey = getPrivateKey(properties.getSigningKey());
        this.issuer = properties.getIssuer();
        this.duration = Duration.parse(properties.getDuration());
        this.kid = properties.getKid();
        this.userService = userService;
        this.productService = productService;
        this.userInstitutionApiRestClient = userInstitutionApiRestClient;
        this.iamExternalRestClient = iamExternalRestClient;
        this.institutionResourceMapper = institutionResourceMapper;
        this.institutionMapper = institutionMapper;
        this.productMapper = productMapper;
    }


    public ExchangedToken exchange(String institutionId, String productId, Optional<String> environment) {
        log.trace("exchange start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchange institutionId = {}, productId = {}", institutionId, productId);
        Authentication authentication = getAuthentication();
        SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
        String userId = selfCareUser.getId();
        UserInstitution userInstitution = getProducts(institutionId, userId);

        Map<String, ProductGrantedAuthority> productGrantedAuthorityMap = retrieveProductsFromInstitutionAndUser(userInstitution);
        final ProductGrantedAuthority productGrantedAuthority = Optional.ofNullable(productGrantedAuthorityMap.get(productId))
                .orElseThrow(() -> new IllegalArgumentException(String.format("A Product Granted SelfCareAuthority is required for product '%s' and institution '%s'", productId, institutionId)));


        it.pagopa.selfcare.dashboard.model.institution.Institution institution = institutionService.getInstitutionById(institutionId);
        Assert.notNull(institution, INSTITUTION_REQUIRED_MESSAGE);
        Institution institutionExchange = institutionResourceMapper.toInstitution(institution, List.of(productGrantedAuthority), false);
        retrieveAndSetGroups(institutionExchange, institutionId, productId, userId);
        TokenExchangeClaims claims = retrieveAndSetClaims(authentication.getCredentials().toString(), institutionExchange, userId, userInstitution.getUserMailUuid());

        Product product = productService.getProduct(productId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getProduct result = {}", product);

        setAudience(claims, product, environment);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Exchanged claims = {}", claims);
        String jwts = createJwts(claims);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Exchanged token = {}", jwts);

        final String urlBO =  retrieveBackofficeUrl(product, environment);

        log.trace("exchange end");
        return new ExchangedToken(jwts, urlBO);
    }

    public ExchangedToken exchangeBackofficeAdmin(String institutionId, String productId, Optional<String> environment) {
        log.trace("exchangeBackofficeAdmin start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchangeBackofficeAdmin institutionId = {}, productId = {}", institutionId, productId);
        Authentication authentication = getAuthentication();
        SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();

        it.pagopa.selfcare.dashboard.model.institution.Institution institution = institutionService.getInstitutionById(institutionId);
        Assert.notNull(institution, INSTITUTION_REQUIRED_MESSAGE);

        final ProductRolePermissionsList permissions = iamExternalRestClient._getIAMProductRolePermissionsList(selfCareUser.getId(), productId).getBody();
        final List<ProductRolePermissions> productRoles = permissions == null || permissions.getItems() == null ? List.of() : permissions.getItems();

        InstitutionBackofficeAdmin institutionExchange = institutionResourceMapper.toInstitutionBackofficeAdmin(institution, productRoles);

        TokenExchangeClaims claims = retrieveAndSetBackofficeAdminClaims(authentication.getCredentials().toString(), institutionExchange,  selfCareUser);

        Product product = productService.getProduct(productId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchangeBackofficeAdmin getProduct result = {}", product);

        setAudience(claims, product, environment);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchangeBackofficeAdmin exchanged claims = {}", claims);
        String jwts = createJwts(claims);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchangeBackofficeAdmin exchanged token = {}", jwts);

        final String urlBO = retrieveBackofficeUrl(product, environment);

        log.trace("exchangeBackofficeAdmin end");
        return new ExchangedToken(jwts, urlBO);
    }

    public ExchangedToken retrieveBillingExchangedToken(String institutionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || authentication.getCredentials() == null) {
            throw new IllegalStateException(AUTHENTICATION_REQUIRED_MESSAGE);
        }

        SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
        String userId = selfCareUser.getId();
        String credentials = authentication.getCredentials().toString();

        List<String> invoiceableProductList = retrieveInvoiceableProductList();

        final List<ProductGrantedAuthority> productGrantedAuthorities = new ArrayList<>();

        UserInstitution userInstitution = getProducts(institutionId, userId);

        Map<String, ProductGrantedAuthority> productGrantedAuthorityMap = retrieveProductsFromInstitutionAndUser(userInstitution);
        addProductIfIsInvoiceable(productGrantedAuthorityMap, invoiceableProductList, productGrantedAuthorities);
        it.pagopa.selfcare.dashboard.model.institution.Institution institutionInfo = institutionService.getInstitutionById(institutionId);
        Assert.notNull(institutionInfo, INSTITUTION_REQUIRED_MESSAGE);
        Institution institution = institutionResourceMapper.toInstitution(institutionInfo, productGrantedAuthorities, true);

        retrieveAndSetGroups(institution, institutionId, null, userId);

        TokenExchangeClaims claims = retrieveAndSetClaims(credentials, institution, userId, userInstitution.getUserMailUuid());
        claims.setAudience(billingAudience);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Exchanged claims = {}", claims);
        String jwts = createJwts(claims);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Exchanged token = {}", jwts);
        log.trace("exchange end");

        return new ExchangedToken(jwts, billingUrl);
    }

    private List<String> retrieveInvoiceableProductList() {
        return productMapper.toTreeResource(productService.getProducts(false,true))
                .stream()
                .flatMap(productTree -> {
                    Stream<Product> nodeStream = Stream.of(productTree.getNode());
                    Stream<Product> childrenStream = productTree.getChildren() != null
                            ? productTree.getChildren().stream()
                            : Stream.empty();
                    return Stream.concat(nodeStream, childrenStream);
                })
                .filter(Product::isInvoiceable)
                .map(Product::getId)
                .toList();
    }

    private TokenExchangeClaims retrieveAndSetClaims(String credential, Institution institution, String userId, String userMailUuid) {
        Claims selcClaims = jwtService.getClaims(credential);
        Assert.notNull(selcClaims, TOKEN_CLAIMS_REQUIRED_MESSAGE);
        TokenExchangeClaims claims = new TokenExchangeClaims(selcClaims);
        claims.setId(UUID.randomUUID().toString());
        claims.setIssuer(issuer);
        User user = userService.getUserById(userId, null, null);

        String email = Optional.ofNullable(user.getWorkContact(userMailUuid))
                .map(workContract -> Objects.nonNull(workContract.getEmail())
                        ? workContract.getEmail().getValue()
                        : ""
                )
                .orElse(null);

        claims.setEmail(email);
        claims.setInstitution(institution);
        claims.setDesiredExpiration(claims.getExpiration());
        claims.setIssuedAt(new Date());
        claims.setExpiration(Date.from(claims.getIssuedAt().toInstant().plus(duration)));
        claims.setSubject(UUID.fromString(userId).toString());
        claims.setType(ID);

        return claims;
    }

    private TokenExchangeClaims retrieveAndSetBackofficeAdminClaims(String credential, InstitutionBackofficeAdmin institution, SelfCareUser selfCareUser) {
        Claims selcClaims = jwtService.getClaims(credential);
        Assert.notNull(selcClaims, TOKEN_CLAIMS_REQUIRED_MESSAGE);
        TokenExchangeClaims claims = new TokenExchangeClaims(selcClaims);
        claims.setId(UUID.randomUUID().toString());

        claims.setIssuer(selfCareUser.getIssuer());

        claims.setEmail(selfCareUser.getEmail());
        claims.setInstitution(institution);

        claims.setDesiredExpiration(claims.getExpiration());
        claims.setIssuedAt(new Date());
        claims.setExpiration(Date.from(claims.getIssuedAt().toInstant().plus(duration)));
        claims.setSubject(UUID.fromString(selfCareUser.getId()).toString());
        claims.setType(ID);

        return claims;
    }

    private Map<String, ProductGrantedAuthority> retrieveProductsFromInstitutionAndUser(UserInstitution userInstitution) {
        Map<String, ProductGrantedAuthority> map = new HashMap<>();
        userInstitution
                .getProducts()
                .stream()
                .filter(product -> product.getStatus().equals(RelationshipState.ACTIVE))
                .collect(groupingBy(OnboardedProduct::getProductId))
                .forEach((key, value) -> map.put(key, new ProductGrantedAuthority(
                        value.get(0).getRole(),
                        value.stream().map(OnboardedProduct::getProductRole).toList(),
                        key)));
        return map;
    }


    private String createJwts(TokenExchangeClaims claims) {
        return Jwts.builder()
                .setClaims(claims.getClaims())
                .signWith(jwtSigningKey, SignatureAlgorithm.RS256)
                .setHeaderParam(JwsHeader.KEY_ID, kid)
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .compact();
    }

    private void retrieveAndSetGroups(Institution institution, String institutionId, String productId, String userId) {
        Page<UserGroup> groupInfos = groupService.getUserGroups(institutionId, productId, UUID.fromString(userId),
                Pageable.ofSize(100));// 100 is a reasonably safe number to retrieve all groups related to a generic user
        if (groupInfos.hasNext()) {
            log.warn("Current user ({}) is member of more than 100 groups related to institution {} and product {}. The Identity Token will contain only the first 100 records",
                    Encode.forJava(userId), Encode.forJava(institutionId), Encode.forJava(productId));
        }
        if (!groupInfos.isEmpty()) {
            institution.setGroups(groupInfos.stream()
                    .map(UserGroup::getId)
                    .toList());
        }
    }

    private void addProductIfIsInvoiceable(
            Map<String, ProductGrantedAuthority> map,
            List<String> productList,
            List<ProductGrantedAuthority> productGrantedAuthorities
    ) {
        map.keySet()
                .forEach(key -> productList.stream().filter(key::equalsIgnoreCase)
                        .findFirst()
                        .ifPresent(productId -> productGrantedAuthorities.add(map.get(key))));
    }


    private PrivateKey getPrivateKey(String signingKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        boolean isRsa = signingKey.contains("RSA");
        String privateKeyEnvelopName = (isRsa ? "RSA " : "") + "PRIVATE KEY";
        String privateKeyPEM = signingKey
                .replace("\r", "")
                .replace("\n", "")
                .replace(String.format(PRIVATE_KEY_HEADER_TEMPLATE, privateKeyEnvelopName), "")
                .replace(String.format(PRIVATE_KEY_FOOTER_TEMPLATE, privateKeyEnvelopName), "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeySpec keySpec;
        if (isRsa) {
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(encoded);
            keySpec = new RSAPrivateCrtKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent(),
                    rsaPrivateKey.getPrivateExponent(),
                    rsaPrivateKey.getPrime1(),
                    rsaPrivateKey.getPrime2(),
                    rsaPrivateKey.getExponent1(),
                    rsaPrivateKey.getExponent2(),
                    rsaPrivateKey.getCoefficient());

        } else {
            keySpec = new PKCS8EncodedKeySpec(encoded);
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private UserInstitution getProducts(String institutionId, String userId) {
        log.trace("getProducts start");
        List<UserInstitutionResponse> institutionResponses = userInstitutionApiRestClient._retrieveUserInstitutions(
                institutionId,
                null,
                null,
                null,
                null,
                userId
        ).getBody();


        if (CollectionUtils.isEmpty(institutionResponses) || institutionResponses.size() != 1){
            throw new ResourceNotFoundException(String.format("InstitutionId %s and userId %s not found", institutionId, userId));
        }

        log.debug("getProducts result = {}", institutionResponses);
        log.trace("getProducts end");
        return institutionMapper.toInstitution(institutionResponses.get(0));
    }

    private Authentication getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication()).orElseThrow(() -> new IllegalStateException(AUTHENTICATION_REQUIRED_MESSAGE));
    }

    private String retrieveBackofficeUrl(Product product, Optional<String> environment) {
        return environment
                .map(env -> product.getBackOfficeEnvironmentConfigurations().get(env).getUrl())
                .orElse(product.getUrlBO());
    }

    private void setAudience(TokenExchangeClaims claims, Product product, Optional<String> environment) {
        environment.ifPresentOrElse(env -> {
            var backOfficeConfigs = product.getBackOfficeEnvironmentConfigurations();
            var envConfig = Optional.ofNullable(backOfficeConfigs)
                    .map(configs -> configs.get(env))
                    .orElseThrow(() -> new InvalidRequestException("Invalid Request"));
            claims.setAudience(envConfig.getIdentityTokenAudience());
        }, () -> claims.setAudience(product.getIdentityTokenAudience()));
    }

    @Data
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Institution implements Serializable {
        private String id;
        @JsonProperty("fiscal_code")
        private String taxCode;
        private String name;
        private List<Role> roles;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<String> groups;
        private String subUnitCode;
        private String subUnitType;
        private String aooParent;
        /**
         * @deprecated This field is deprecated and may be removed in a future version.
         */
        @Deprecated(since = "1.1.8", forRemoval = false)
        private String parentDescription; // NOSONAR
        @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = RootParent.class)
        private RootParent rootParent;
        @JsonProperty("ipaCode")
        private String originId;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Role implements Serializable {
        private PartyRole partyRole;
        @JsonProperty("role")
        private String productRole;
        private String productId;
    }

    static class TokenExchangeClaims {
        public static final String DESIRED_EXPIRATION = "desired_exp";
        public static final String INSTITUTION = "organization";
        public static final String EMAIL = "email";
        public static final String TYPE = "typ";

        private final Claims claims;

        public TokenExchangeClaims(Claims claims) {
            this.claims = claims;
        }

        public Claims getClaims() {
            return claims;
        }

        public void setId(String id) {
            claims.setId(id);
        }

        public void setIssuer(String issuer) {
            claims.setIssuer(issuer);
        }

        public void setSubject(String subject) {
            claims.setSubject(subject);
        }

        public void setAudience(String audience) {
            claims.setAudience(audience);
        }

        public void setIssuedAt(Date issuedAt) {
            claims.setIssuedAt(issuedAt);
        }

        public Date getIssuedAt() {
            return claims.getIssuedAt();
        }

        public void setExpiration(Date expiration) {
            claims.setExpiration(expiration);
        }

        public Date getExpiration() {
            return claims.getExpiration();
        }

        public void setDesiredExpiration(Date desiredExp) {
            long seconds = desiredExp.getTime() / 1000L;
            claims.put(DESIRED_EXPIRATION, seconds);
        }

        public void setInstitution(Institution institution) {
            claims.put(INSTITUTION, institution);
        }

        public void setInstitution(InstitutionBackofficeAdmin institution) {
            claims.put(INSTITUTION, institution);
        }

        public void setEmail(String email) {
            claims.put(EMAIL, email);
        }

        public void setType(String type) {
            claims.put(TYPE, type);
        }

    }

}
