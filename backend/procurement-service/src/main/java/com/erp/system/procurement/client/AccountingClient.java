package com.erp.system.procurement.client;

import com.erp.system.procurement.dto.AccountingPostingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "finance-service", path = "/api/accounting/postings")
public interface AccountingClient {

    @PostMapping
    void post(@RequestBody AccountingPostingRequest request);
}
