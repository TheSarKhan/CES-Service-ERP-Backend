package com.ces.service.module.customer.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.customer.dto.CustomerRequest;
import com.ces.service.module.customer.dto.CustomerResponse;
import com.ces.service.module.customer.entity.Customer;
import com.ces.service.module.customer.enums.CustomerType;
import com.ces.service.module.customer.repository.CustomerRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal Customer slice (M04) — search + create, built for the Qaraj vehicle wizard's owner
 * picker rather than as the full customer-management module. See {@link Customer}'s javadoc.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String search, boolean activeOnly, Pageable pageable) {
        UUID branchId = BranchContext.get();
        return customerRepository.search(branchId, activeOnly, search, pageable).map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return CustomerResponse.from(loadCustomer(id));
    }

    public CustomerResponse create(CustomerRequest request) {
        UUID branchId = BranchContext.get();
        String voen = blankToNull(request.getVoen());
        if (voen != null && customerRepository.existsByVoenAndBranchIdAndDeletedAtIsNull(voen, branchId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VOEN);
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .companyName(blankToNull(request.getCompanyName()))
                .voen(voen)
                .phone(blankToNull(request.getPhone()))
                .email(blankToNull(request.getEmail()))
                .address(blankToNull(request.getAddress()))
                .customerType(request.getCustomerType() != null ? request.getCustomerType() : CustomerType.INDIVIDUAL)
                .notes(request.getNotes())
                .build();
        customer.setBranchId(branchId);

        return CustomerResponse.from(customerRepository.save(customer));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Customer loadCustomer(UUID id) {
        return customerRepository.findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
