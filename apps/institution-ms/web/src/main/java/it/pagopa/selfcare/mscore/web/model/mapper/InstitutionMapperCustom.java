package it.pagopa.selfcare.mscore.web.model.mapper;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.web.model.institution.*;
import it.pagopa.selfcare.mscore.web.model.onboarding.OnboardedProducts;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.NONE)
public class InstitutionMapperCustom {

    protected static final BinaryOperator<BulkProduct> MERGE_FUNCTION = (inst1, inst2) -> inst1.getStatus().compareTo(inst2.getStatus()) < 0 ? inst1 : inst2;

    public static InstitutionBillingResponse toInstitutionBillingResponse(Institution institution, String productId) {
        if (institution == null) {
            return null;
        }
        InstitutionBillingResponse response = new InstitutionBillingResponse();

        response.setInstitutionId(institution.getId());
        response.setExternalId(institution.getExternalId());
        response.setDescription(institution.getDescription());
        response.setInstitutionType(toInstitutionType(institution, productId));
        response.setDigitalAddress(institution.getDigitalAddress());
        response.setAddress(institution.getAddress());
        response.setZipCode(institution.getZipCode());
        response.setTaxCode(institution.getTaxCode());

        response.setSubunitCode(institution.getSubunitCode());
        response.setSubunitType(institution.getSubunitType());
        response.setAooParentCode(Optional.ofNullable(institution.getPaAttributes()).map(PaAttributes::getAooParentCode).orElse(null));

        for (Onboarding onboarding : institution.getOnboarding()) {
            if (productId.equalsIgnoreCase(onboarding.getProductId())) {
                response.setBilling(toBillingResponse(onboarding.getBilling(), institution));
                response.setPricingPlan(onboarding.getPricingPlan());
            }
        }

        return response;
    }

    private static Optional<Onboarding> getOnboarding(Institution institution, String productId) {
        return Optional.ofNullable(productId)
                .flatMap(pid -> Optional.ofNullable(institution.getOnboarding()))
                .flatMap(onb -> onb.stream().filter(o -> o.getProductId().equals(productId)).findFirst());
    }

    private static String toInstitutionType(Institution institution, String productId) {
        return  getOnboarding(institution, productId)
                .flatMap(o -> Optional.ofNullable(o.getInstitutionType()))
                .map(InstitutionType::name)
                .orElse(null);
    }

    private static String toOriginId(Institution institution, String productId) {
        return getOnboarding(institution, productId)
                .map(Onboarding::getOriginId)
                .orElse(institution.getOriginId());
    }

    private static String toOrigin(Institution institution, String productId) {
        return getOnboarding(institution, productId)
                .map(Onboarding::getOrigin)
                .orElse(institution.getOrigin());
    }

    private static BillingResponse toBillingResponse(Billing billing, Institution institution) {
        BillingResponse billingResponse = new BillingResponse();
        if (billing != null) {
            billingResponse.setVatNumber(billing.getVatNumber());
            billingResponse.setTaxCodeInvoicing(billing.getTaxCodeInvoicing());
            billingResponse.setRecipientCode(billing.getRecipientCode());
            billingResponse.setPublicServices(billing.isPublicServices());
        } else if (institution.getBilling() != null) {
            billingResponse.setVatNumber(institution.getBilling().getVatNumber());
            billingResponse.setTaxCodeInvoicing(institution.getBilling().getTaxCodeInvoicing());
            billingResponse.setRecipientCode(institution.getBilling().getRecipientCode());
            billingResponse.setPublicServices(institution.getBilling().isPublicServices());
        }
        return billingResponse;
    }

    public static DataProtectionOfficerResponse toDataProtectionOfficerResponse(DataProtectionOfficer dataProtectionOfficer) {
        DataProtectionOfficerResponse response = null;
        if (dataProtectionOfficer != null) {
            response = new DataProtectionOfficerResponse();
            response.setPec(dataProtectionOfficer.getPec());
            response.setEmail(dataProtectionOfficer.getEmail());
            response.setAddress(dataProtectionOfficer.getAddress());
        }
        return response;
    }

    public static PaymentServiceProviderResponse toPaymentServiceProviderResponse(PaymentServiceProvider paymentServiceProvider) {
        PaymentServiceProviderResponse response = null;
        if (paymentServiceProvider != null) {
            response = new PaymentServiceProviderResponse();
            response.setAbiCode(paymentServiceProvider.getAbiCode());
            response.setLegalRegisterName(paymentServiceProvider.getLegalRegisterName());
            response.setBusinessRegisterNumber(paymentServiceProvider.getBusinessRegisterNumber());
            response.setVatNumberGroup(paymentServiceProvider.isVatNumberGroup());
            response.setLegalRegisterNumber(paymentServiceProvider.getLegalRegisterNumber());
        }
        return response;
    }

    public static List<AttributesResponse> toAttributeResponse(List<Attributes> attributes) {
        List<AttributesResponse> list = new ArrayList<>();
        if (attributes != null && !attributes.isEmpty()) {
            for (Attributes a : attributes) {
                AttributesResponse response = new AttributesResponse();
                response.setCode(a.getCode());
                response.setOrigin(a.getOrigin());
                response.setDescription(a.getDescription());
                list.add(response);
            }
        }
        return list;
    }

    public static List<GeoTaxonomies> toGeoTaxonomies(List<InstitutionGeographicTaxonomies> geographicTaxonomies) {
        List<GeoTaxonomies> list = new ArrayList<>();
        if (geographicTaxonomies != null) {
            for (InstitutionGeographicTaxonomies g : geographicTaxonomies) {
                GeoTaxonomies geoTaxonomies = new GeoTaxonomies();
                geoTaxonomies.setCode(g.getCode());
                geoTaxonomies.setDesc(g.getDesc());
                list.add(geoTaxonomies);
            }
        }
        return list;
    }

    public static OnboardedProducts toOnboardedProducts(List<Onboarding> page) {
        OnboardedProducts onboardedProducts = new OnboardedProducts();
        onboardedProducts.setProducts(toInstitutionProduct(page));
        return onboardedProducts;
    }

    public static List<InstitutionProduct> toInstitutionProduct(List<Onboarding> onboardings) {
        if (onboardings == null) {
            return Collections.emptyList();
        }
        return onboardings.stream().map(onboarding -> {
            InstitutionProduct product = new InstitutionProduct();
            product.setId(onboarding.getProductId());
            product.setState(onboarding.getStatus());
            return product;
        }).toList();
    }

    public static InstitutionOnboardingResponse toInstitutionOnboardingResponse(Institution institution, String productId) {
        InstitutionOnboardingResponse response = new InstitutionOnboardingResponse();
        response.setId(institution.getId());
        response.setExternalId(institution.getExternalId());
        response.setOrigin(toOrigin(institution, productId));
        response.setOriginId(toOriginId(institution, productId));
        response.setDescription(institution.getDescription());
        response.setInstitutionType(toInstitutionType(institution, productId));
        response.setDigitalAddress(institution.getDigitalAddress());
        response.setAddress(institution.getAddress());
        response.setZipCode(institution.getZipCode());
        response.setTaxCode(institution.getTaxCode());
        if (institution.getOnboarding() != null) {
            response.setOnboardings(toOnboardingMap(institution.getOnboarding(), institution));
        }
        if (institution.getGeographicTaxonomies() != null) {
            response.setGeographicTaxonomies(toGeoTaxonomies(institution.getGeographicTaxonomies()));
        }
        if (institution.getAttributes() != null) {
            response.setAttributes(toAttributeResponse(institution.getAttributes()));
        }
        if (institution.getPaymentServiceProvider() != null) {
            response.setPaymentServiceProvider(toPaymentServiceProviderResponse(institution.getPaymentServiceProvider()));
        }
        if (institution.getDataProtectionOfficer() != null) {
            response.setDataProtectionOfficer(toDataProtectionOfficerResponse(institution.getDataProtectionOfficer()));
        }
        response.setRea(institution.getRea());
        response.setShareCapital(institution.getShareCapital());
        response.setBusinessRegisterPlace(institution.getBusinessRegisterPlace());
        response.setSupportEmail(institution.getSupportEmail());
        response.setSupportPhone(institution.getSupportPhone());
        response.setImported(institution.isImported());
        response.setCreatedAt(institution.getCreatedAt());
        response.setUpdatedAt(institution.getUpdatedAt());
        response.setSubunitCode(institution.getSubunitCode());
        response.setSubunitType(institution.getSubunitType());
        response.setAooParentCode(Optional.ofNullable(institution.getPaAttributes()).map(PaAttributes::getAooParentCode).orElse(null));
        response.setInstitutionType(toInstitutionType(institution, productId));
        return response;
    }

    private static Map<String, OnboardingResponse> toOnboardingMap(List<Onboarding> onboarding, Institution institution) {
        Map<String, OnboardingResponse> map = new HashMap<>();
        if (onboarding != null) {
            for (Onboarding o : onboarding) {
                OnboardingResponse onboardingResponse = new OnboardingResponse();
                onboardingResponse.setProductId(o.getProductId());
                onboardingResponse.setTokenId(o.getTokenId());
                onboardingResponse.setStatus(o.getStatus());
                onboardingResponse.setContract(o.getContract());
                onboardingResponse.setPricingPlan(o.getPricingPlan());
                onboardingResponse.setBilling(toBillingResponse(o.getBilling(), institution));
                onboardingResponse.setCreatedAt(o.getCreatedAt());
                onboardingResponse.setUpdatedAt(o.getUpdatedAt());
                onboardingResponse.setClosedAt(o.getClosedAt());
                onboardingResponse.setIsAggregator(o.getIsAggregator());
                onboardingResponse.setInstitutionType(o.getInstitutionType());
                onboardingResponse.setOrigin(o.getOrigin());
                onboardingResponse.setOriginId(o.getOriginId());
                if (!map.containsKey(o.getProductId()) ||
                        map.containsKey(o.getProductId()) && map.get(o.getProductId()).getStatus() != RelationshipState.ACTIVE) {
                    map.put(o.getProductId(), onboardingResponse);
                }
            }
        }
        return map;
    }

    public static List<InstitutionToOnboard> toInstitutionToOnboardList(List<ValidInstitution> validInstitutions) {
        return validInstitutions.stream()
                .map(InstitutionMapperCustom::toInstitutionToOnboard)
                .toList();
    }

    public static InstitutionToOnboard toInstitutionToOnboard(ValidInstitution validInstitutions) {
        InstitutionToOnboard institution = new InstitutionToOnboard();
        institution.setDescription(validInstitutions.getDescription());
        institution.setId(validInstitutions.getId());
        return institution;
    }

    public static List<ValidInstitution> toValidInstitutions(List<InstitutionToOnboard> institutions) {
        return institutions.stream()
                .map(institutionToOnboard -> new ValidInstitution(institutionToOnboard.getId(), institutionToOnboard.getDescription()))
                .toList();
    }

    public static BulkInstitutions toBulkInstitutions(List<Institution> institution, List<String> idsRequest) {
        BulkInstitutions bulkInstitutions = new BulkInstitutions();
        bulkInstitutions.setFound(institution.stream()
                .map(InstitutionMapperCustom::toBulkInstitution)
                .toList());
        bulkInstitutions.setNotFound(idsRequest.stream()
                .filter(s -> institution.stream().noneMatch(inst -> inst.getId().equalsIgnoreCase(s)))
                .toList());
        return bulkInstitutions;
    }

    private static BulkInstitution toBulkInstitution(Institution inst) {
        BulkInstitution bulkInstitution = new BulkInstitution();
        bulkInstitution.setId(inst.getId());
        bulkInstitution.setExternalId(inst.getExternalId());
        bulkInstitution.setOrigin(inst.getOrigin());
        bulkInstitution.setOriginId(inst.getOriginId());
        bulkInstitution.setDescription(inst.getDescription());
        bulkInstitution.setDigitalAddress(inst.getDigitalAddress());
        bulkInstitution.setAddress(inst.getAddress());
        bulkInstitution.setZipCode(inst.getZipCode());
        bulkInstitution.setTaxCode(inst.getTaxCode());
        bulkInstitution.setAttributes(toAttributeResponse(inst.getAttributes()));
        bulkInstitution.setProducts(toBulkProductMap(inst.getOnboarding(), inst));
        return bulkInstitution;
    }

    private static Map<String, BulkProduct> toBulkProductMap(List<Onboarding> onboarding, Institution institution) {
        if(onboarding != null && !onboarding.isEmpty()) {
            return onboarding.stream().map(onb -> {
                BulkProduct bulkProduct = new BulkProduct();
                bulkProduct.setProduct(onb.getProductId());
                bulkProduct.setPricingPlan(onb.getPricingPlan());
                bulkProduct.setBilling(toBillingResponse(onb.getBilling(), institution));
                bulkProduct.setStatus(onb.getStatus());
                bulkProduct.setInstitutionType(onb.getInstitutionType());
                bulkProduct.setOrigin(onb.getOrigin());
                bulkProduct.setOriginId(onb.getOriginId());
                return bulkProduct;
            }).collect(Collectors.toMap(BulkProduct::getProduct, Function.identity(), MERGE_FUNCTION));
        }
        return Collections.emptyMap();
    }
}
