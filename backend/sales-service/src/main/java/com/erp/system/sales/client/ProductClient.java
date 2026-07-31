package com.erp.system.sales.client;

import com.erp.system.sales.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/api/products")
public interface ProductClient {

    @GetMapping("/by-sku/{sku}")
    ProductDto getProductBySku(@PathVariable("sku") String sku);
}
