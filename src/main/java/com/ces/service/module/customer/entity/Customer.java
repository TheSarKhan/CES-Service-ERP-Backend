package com.ces.service.module.customer.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.customer.enums.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A customer (M04) — the {@code customers} table (V8) predates this entity, which is the minimal
 * slice the Qaraj "Yeni texnika" wizard needs for its owner picker: search + create. Full M04
 * (edit, ERP sync) stays the still-unbuilt {@code /customers} module page's job — see
 * {@code VehicleService.validateOwnership}'s comment, which anticipated exactly this addition.
 */
@Entity
@Table(name = "customers", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Customer extends BaseEntity {

    /** Set only once CES ERP sync (M13) exists — always null today. */
    @Column(name = "erp_customer_id")
    private UUID erpCustomerId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "company_name")
    private String companyName;

    /** Unique per branch among non-deleted rows when given — enforced by a DB partial index. */
    @Column(name = "voen")
    private String voen;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    @Builder.Default
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "notes")
    private String notes;
}
