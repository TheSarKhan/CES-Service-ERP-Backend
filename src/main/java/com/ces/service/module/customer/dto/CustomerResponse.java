package com.ces.service.module.customer.dto;

import com.ces.service.module.customer.entity.Customer;
import com.ces.service.module.customer.enums.CustomerType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private UUID id;
    private String fullName;
    private String companyName;
    private String voen;
    private String phone;
    private String email;
    private String address;
    private CustomerType customerType;
    private Boolean isActive;
    private String notes;
    private Instant createdAt;

    public static CustomerResponse from(Customer c) {
        return CustomerResponse.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .companyName(c.getCompanyName())
                .voen(c.getVoen())
                .phone(c.getPhone())
                .email(c.getEmail())
                .address(c.getAddress())
                .customerType(c.getCustomerType())
                .isActive(c.getIsActive())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
