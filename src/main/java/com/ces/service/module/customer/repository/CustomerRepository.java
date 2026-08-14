package com.ces.service.module.customer.repository;

import com.ces.service.module.customer.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    boolean existsByVoenAndBranchIdAndDeletedAtIsNull(String voen, UUID branchId);

    /**
     * Every text filter param is wrapped in {@code cast(:x as string)} at EVERY use — see
     * {@code VehicleRepository.search}'s comment for why a bare {@code :x IS NULL} breaks PgJDBC's
     * type inference and sends the parameter as bytea.
     */
    @Query("""
            select c from Customer c
            where c.branchId = :branchId
              and c.deletedAt is null
              and (:activeOnly = false or c.isActive = true)
              and (cast(:search as string) is null
                   or lower(c.fullName) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(c.companyName, '')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(c.voen, '')) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(c.phone, '')) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Customer> search(
            @Param("branchId") UUID branchId,
            @Param("activeOnly") boolean activeOnly,
            @Param("search") String search,
            Pageable pageable);
}
