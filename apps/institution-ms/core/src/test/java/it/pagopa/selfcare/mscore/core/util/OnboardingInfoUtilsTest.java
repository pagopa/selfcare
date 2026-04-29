package it.pagopa.selfcare.mscore.core.util;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.mscore.constant.Env;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.Billing;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.institution.Premium;
import it.pagopa.selfcare.mscore.model.onboarding.OnboardedProduct;
import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.model.user.UserBinding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OnboardingInfoUtilsTest {

    /**
     * Method under test: {@link OnboardingInfoUtils#convertStatesToRelationshipsState(String[])}
     */
    @Test
    void testConvertStatesToRelationshipsState2() {
        assertTrue(OnboardingInfoUtils.convertStatesToRelationshipsState(new String[]{}).isEmpty());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn() {
        ArrayList<UserBinding> userInstitutionToBeFiltered = new ArrayList<>();
        assertThrows(ResourceNotFoundException.class, () -> OnboardingInfoUtils
                .getUserInstitutionsWithProductStatusIn(userInstitutionToBeFiltered, new ArrayList<>()));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn2() {
        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(new ArrayList<>());

        ArrayList<UserBinding> userBindingList = new ArrayList<>();
        userBindingList.add(userBinding);
        assertThrows(ResourceNotFoundException.class,
                () -> OnboardingInfoUtils.getUserInstitutionsWithProductStatusIn(userBindingList, new ArrayList<>()));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn3() {
        ArrayList<UserBinding> userInstitutionToBeFiltered = new ArrayList<>();

        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        relationshipStateList.add(RelationshipState.PENDING);
        assertThrows(ResourceNotFoundException.class, () -> OnboardingInfoUtils
                .getUserInstitutionsWithProductStatusIn(userInstitutionToBeFiltered, relationshipStateList));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn4() {
        ArrayList<UserBinding> userInstitutionToBeFiltered = new ArrayList<>();

        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        relationshipStateList.add(RelationshipState.PENDING);
        relationshipStateList.add(RelationshipState.PENDING);
        assertThrows(ResourceNotFoundException.class, () -> OnboardingInfoUtils
                .getUserInstitutionsWithProductStatusIn(userInstitutionToBeFiltered, relationshipStateList));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn5() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract(", ");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        ArrayList<UserBinding> userBindingList = new ArrayList<>();
        userBindingList.add(userBinding);
        assertThrows(ResourceNotFoundException.class,
                () -> OnboardingInfoUtils.getUserInstitutionsWithProductStatusIn(userBindingList, new ArrayList<>()));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn6() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract(", ");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        OnboardedProduct onboardedProduct1 = new OnboardedProduct();
        onboardedProduct1.setContract(", ");
        onboardedProduct1.setCreatedAt(null);
        onboardedProduct1.setEnv(Env.ROOT);
        onboardedProduct1.setProductId("42");
        onboardedProduct1.setProductRole("");
        onboardedProduct1.setRole(PartyRole.MANAGER);
        onboardedProduct1.setStatus(RelationshipState.PENDING);
        onboardedProduct1.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct1);
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        ArrayList<UserBinding> userBindingList = new ArrayList<>();
        userBindingList.add(userBinding);
        assertThrows(ResourceNotFoundException.class,
                () -> OnboardingInfoUtils.getUserInstitutionsWithProductStatusIn(userBindingList, new ArrayList<>()));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getUserInstitutionsWithProductStatusIn(List, List)}
     */
    @Test
    void testGetUserInstitutionsWithProductStatusIn7() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract(", ");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        ArrayList<UserBinding> userBindingList = new ArrayList<>();
        userBindingList.add(userBinding);

        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        relationshipStateList.add(RelationshipState.PENDING);
        List<UserBinding> actualUserInstitutionsWithProductStatusIn = OnboardingInfoUtils
                .getUserInstitutionsWithProductStatusIn(userBindingList, relationshipStateList);
        assertEquals(1, actualUserInstitutionsWithProductStatusIn.size());
        assertEquals(onboardedProductList, actualUserInstitutionsWithProductStatusIn.get(0).getProducts());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#findOnboardingLinkedToProductWithStateIn(UserBinding, Institution, List)}
     */
    @Test
    void testFindOnboardingLinkedToProductWithStateIn() {
        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        userBinding.setProducts(onboardedProductList);
        Institution onboardedInstitution = new Institution();
        assertNotNull(
                OnboardingInfoUtils
                        .findOnboardingLinkedToProductWithStateIn(userBinding, onboardedInstitution, new ArrayList<>())
                        .getOnboarding());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#findOnboardingLinkedToProductWithStateIn(UserBinding, Institution, List)}
     */
    @Test
    void testFindOnboardingLinkedToProductWithStateIn6() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract("Contract");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        Premium premium = new Premium();
        premium.setContract("Contract");
        premium.setStatus(RelationshipState.PENDING);

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setContract("Contract");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("Pricing Plan");
        onboarding.setProductId("42");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding);

        Institution institution = new Institution();
        institution.setOnboarding(onboardingList);
        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        List<Onboarding> onboarding1 = OnboardingInfoUtils
                .findOnboardingLinkedToProductWithStateIn(userBinding, institution, relationshipStateList)
                .getOnboarding();
        assertNotNull(onboarding1);
        assertTrue(onboarding1.isEmpty());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#findOnboardingLinkedToProductWithStateIn(UserBinding, Institution, List)}
     */
    @Test
    void testFindOnboardingLinkedToProductWithStateIn7() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract("Contract");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        Premium premium = new Premium();
        premium.setContract("Contract");
        premium.setStatus(RelationshipState.PENDING);

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setContract("Contract");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("Pricing Plan");
        onboarding.setProductId("42");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setUpdatedAt(null);

        Billing billing1 = new Billing();
        billing1.setPublicServices(true);
        billing1.setRecipientCode("Recipient Code");
        billing1.setVatNumber("42");

        Premium premium1 = new Premium();
        premium1.setContract("Contract");
        premium1.setStatus(RelationshipState.PENDING);

        Onboarding onboarding1 = new Onboarding();
        onboarding1.setBilling(billing1);
        onboarding1.setContract("Contract");
        onboarding1.setCreatedAt(null);
        onboarding1.setPricingPlan("Pricing Plan");
        onboarding1.setProductId("42");
        onboarding1.setStatus(RelationshipState.PENDING);
        onboarding1.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding1);
        onboardingList.add(onboarding);

        Institution institution = new Institution();
        institution.setOnboarding(onboardingList);
        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        List<Onboarding> onboarding2 = OnboardingInfoUtils
                .findOnboardingLinkedToProductWithStateIn(userBinding, institution, relationshipStateList)
                .getOnboarding();
        assertTrue(onboarding2.isEmpty());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#findOnboardingLinkedToProductWithStateIn(UserBinding, Institution, List)}
     */
    @Test
    void testFindOnboardingLinkedToProductWithStateIn8() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract("Contract");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        Premium premium = new Premium();
        premium.setContract("Contract");
        premium.setStatus(RelationshipState.PENDING);

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setContract("Contract");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("Pricing Plan");
        onboarding.setProductId("Product Id");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding);

        Institution institution = new Institution();
        institution.setOnboarding(onboardingList);
        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        List<Onboarding> onboarding1 = OnboardingInfoUtils
                .findOnboardingLinkedToProductWithStateIn(userBinding, institution, relationshipStateList)
                .getOnboarding();
        assertTrue(onboarding1.isEmpty());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#findOnboardingLinkedToProductWithStateIn(UserBinding, Institution, List)}
     */
    @Test
    void testFindOnboardingLinkedToProductWithStateIn9() {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setContract("Contract");
        onboardedProduct.setCreatedAt(null);
        onboardedProduct.setEnv(Env.ROOT);
        onboardedProduct.setProductId("42");
        onboardedProduct.setProductRole("");
        onboardedProduct.setRole(PartyRole.MANAGER);
        onboardedProduct.setStatus(RelationshipState.PENDING);
        onboardedProduct.setUpdatedAt(null);

        ArrayList<OnboardedProduct> onboardedProductList = new ArrayList<>();
        onboardedProductList.add(onboardedProduct);

        UserBinding userBinding = new UserBinding();

        userBinding.setInstitutionId("42");
        userBinding.setProducts(onboardedProductList);

        Billing billing = new Billing();
        billing.setPublicServices(true);
        billing.setRecipientCode("Recipient Code");
        billing.setVatNumber("42");

        Premium premium = new Premium();
        premium.setContract("Contract");
        premium.setStatus(RelationshipState.PENDING);

        Onboarding onboarding = new Onboarding();
        onboarding.setBilling(billing);
        onboarding.setContract("Contract");
        onboarding.setCreatedAt(null);
        onboarding.setPricingPlan("Pricing Plan");
        onboarding.setProductId("42");
        onboarding.setStatus(RelationshipState.PENDING);
        onboarding.setUpdatedAt(null);

        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        onboardingList.add(onboarding);

        Institution institution = new Institution();
        institution.setOnboarding(onboardingList);

        ArrayList<RelationshipState> relationshipStateList = new ArrayList<>();
        relationshipStateList.add(RelationshipState.PENDING);
        List<Onboarding> onboarding1 = OnboardingInfoUtils
                .findOnboardingLinkedToProductWithStateIn(userBinding, institution, relationshipStateList)
                .getOnboarding();
        assertEquals(onboardingList, onboarding1);
        assertEquals(1, onboarding1.size());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getRelationShipStateList(String[])}
     */
    @Test
    void testGetRelationShipStateList() {
        String[] states = {"ACTIVE"};
        List<RelationshipState> actualRelationShipStateList = OnboardingInfoUtils.getRelationShipStateList(states);
        assertEquals(1, actualRelationShipStateList.size());
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getRelationShipStateList(String[])}
     */
    @Test
    void testGetRelationShipStateList2() {
        List<RelationshipState> actualRelationShipStateList = OnboardingInfoUtils.getRelationShipStateList(null);
        assertEquals(2, actualRelationShipStateList.size());
        assertEquals(RelationshipState.ACTIVE, actualRelationShipStateList.get(0));
        assertEquals(RelationshipState.PENDING, actualRelationShipStateList.get(1));
    }

    /**
     * Method under test: {@link OnboardingInfoUtils#getRelationShipStateList(String[])}
     */
    @Test
    void testGetRelationShipStateList3() {
        List<RelationshipState> actualRelationShipStateList = OnboardingInfoUtils
                .getRelationShipStateList(new String[]{});
        assertEquals(2, actualRelationShipStateList.size());
        assertEquals(RelationshipState.ACTIVE, actualRelationShipStateList.get(0));
        assertEquals(RelationshipState.PENDING, actualRelationShipStateList.get(1));
    }
}

