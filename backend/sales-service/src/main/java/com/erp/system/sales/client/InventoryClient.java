package com.erp.system.sales.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryClient {

    @PutMapping("/stock/{sku}")
    void deductStock(@PathVariable("sku") String sku, @RequestParam("quantityChange") Integer quantityChange);
}
