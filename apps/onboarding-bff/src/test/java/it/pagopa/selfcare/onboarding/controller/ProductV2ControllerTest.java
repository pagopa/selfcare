package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.OriginResult;
import it.pagopa.selfcare.onboarding.controller.response.OriginResponse;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductV2ControllerTest {

    @Mock
    ProductService productService;
    @Mock
    InstitutionMapper institutionMapper;

    @InjectMocks
    ProductV2Controller productV2Controller;

    @Test
    void getOrigins_withProductId_returnsMappedResponse() {
        OriginResult originResult = new OriginResult();
        OriginResponse expected = new OriginResponse();
        when(productService.getOrigins("productId")).thenReturn(originResult);
        when(institutionMapper.toOriginResponse(originResult)).thenReturn(expected);

        OriginResponse response = productV2Controller.getOrigins("productId");

        assertSame(expected, response);
        verify(productService).getOrigins("productId");
    }

    @Test
    void getOrigins_withoutProductId_stillDelegatesToService() {
        OriginResult originResult = new OriginResult();
        OriginResponse expected = new OriginResponse();
        when(productService.getOrigins(null)).thenReturn(originResult);
        when(institutionMapper.toOriginResponse(originResult)).thenReturn(expected);

        OriginResponse response = productV2Controller.getOrigins(null);

        assertSame(expected, response);
        verify(productService).getOrigins(null);
    }
}
