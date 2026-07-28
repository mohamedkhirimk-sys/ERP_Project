package com.erp.system.product.controller;

import com.erp.common.exception.GlobalExceptionHandler;
import com.erp.common.exception.ResourceNotFoundException;
import com.erp.system.product.entity.Product;
import com.erp.system.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getProductById_shouldReturn200_whenFound() throws Exception {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        when(productService.getProductById(1L)).thenReturn(product);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_shouldReturn404_whenNotFound() throws Exception {
        when(productService.getProductById(1L))
                .thenThrow(new ResourceNotFoundException("Product", 1L));
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }
}
