package com.ces.service.module.customer.dto;

import com.ces.service.module.customer.enums.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CustomerRequest {

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String companyName;

    @Size(max = 20)
    private String voen;

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String email;

    private String address;

    /** Null means INDIVIDUAL. */
    private CustomerType customerType;

    private String notes;
}
