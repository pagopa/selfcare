package it.pagopa.selfcare.onboarding.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.onboarding.core.ProductAzureService;
import it.pagopa.selfcare.onboarding.web.config.WebTestConfig;
import it.pagopa.selfcare.onboarding.web.model.ProductResource;
import it.pagopa.selfcare.onboarding.web.model.mapper.ProductMapperImpl;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(value = {ProductController.class}, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
        ProductController.class,
        WebTestConfig.class,
        ProductMapperImpl.class
})
class ProductControllerTest {

    private static final String BASE_URL = "/v1/product";

    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected ProductAzureService productAzureServiceMock;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Method under test: {@link ProductController#getProduct(String, Optional)}
     */
    @Test
    void getProduct() throws Exception {
        //given
        final String productId = "productId";
        Mockito.when(productAzureServiceMock.getProduct(Mockito.any(), any()))
                .thenReturn(new Product());
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        //then
        ProductResource response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProductResource.class);
        Assertions.assertNotNull(response);
        Mockito.verify(productAzureServiceMock, Mockito.times(1))
                .getProduct(Mockito.anyString(), any());
        Mockito.verifyNoMoreInteractions(productAzureServiceMock);
    }

    /**
     * Method under test: {@link ProductController#getProducts()}
     */
    @Test
    void getProducts() throws Exception {
        //given
        Mockito.when(productAzureServiceMock.getProducts(false))
                .thenReturn(List.of(new Product()));
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/v1/products")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        //then
        List<ProductResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>(){});
        Assertions.assertNotNull(response);
        Mockito.verify(productAzureServiceMock, Mockito.times(1))
                .getProducts(false);
        Mockito.verifyNoMoreInteractions(productAzureServiceMock);
    }

    /**
     * Method under test: {@link ProductController#getProductsAdmin()}
     */
    @Test
    void getProductsAdmin() throws Exception {
        //given
        Product product = new Product();
        Map<String, ContractTemplate> userContractMappings = new HashMap<>();
        ContractTemplate userContract = new ContractTemplate();
        userContract.setContractTemplatePath("test");
        userContract.setContractTemplateVersion("version");
        userContractMappings.put(Product.CONTRACT_TYPE_DEFAULT, userContract);
        product.setUserContractMappings(userContractMappings);

        Mockito.when(productAzureServiceMock.getProducts(true))
                .thenReturn(List.of(product));
        //when
        MvcResult result = mvc.perform(MockMvcRequestBuilders
                        .get("/v1/products/admin")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        //then
        List<ProductResource> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>(){});
        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.size());
        Mockito.verify(productAzureServiceMock, Mockito.times(1))
                .getProducts(true);
        Mockito.verifyNoMoreInteractions(productAzureServiceMock);
    }
}
