package com.ces.service.infrastructure.erp;

import com.ces.service.infrastructure.erp.dto.AccountingEntryRequest;
import com.ces.service.infrastructure.erp.dto.ErpCustomerRequest;
import com.ces.service.infrastructure.erp.dto.ErpCustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client for the CES ERP REST API (SRS §M13). Two integration points:
 * bidirectional customer synchronisation, and unidirectional accounting push.
 */
@FeignClient(
        name = "ces-erp",
        url = "${ces.erp.base-url}",
        configuration = ErpFeignConfig.class
)
public interface ErpApiClient {

    @PostMapping("/api/customers")
    ErpCustomerResponse createCustomer(@RequestBody ErpCustomerRequest request);

    @PutMapping("/api/customers/{erpId}")
    ErpCustomerResponse updateCustomer(@PathVariable("erpId") UUID erpId,
                                       @RequestBody ErpCustomerRequest request);

    @GetMapping("/api/customers")
    Page<ErpCustomerResponse> getCustomers(@RequestParam("page") int page,
                                           @RequestParam("size") int size);

    @PostMapping("/api/accounting/entries")
    void pushAccountingEntry(@RequestBody AccountingEntryRequest request);
}
