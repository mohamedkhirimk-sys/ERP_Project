package com.erp.system.hrm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "identity-service", path = "/api/users")
public interface IdentityClient {

    @PutMapping("/{id}/deactivate")
    void deactivateUser(@PathVariable("id") Long id);
}
