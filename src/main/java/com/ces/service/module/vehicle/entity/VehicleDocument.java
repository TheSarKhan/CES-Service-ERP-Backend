package com.ces.service.module.vehicle.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Document attached to a vehicle ({@code vehicle_documents}, M03). Extends {@link BaseEntity}
 * (branch_id + audit + soft delete). {@code vehicle_id} references the owning vehicle.
 */
@Entity
@Table(name = "vehicle_documents", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VehicleDocument extends BaseEntity {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "doc_type", nullable = false, length = 100)
    private String docType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "expires_at")
    private LocalDate expiresAt;
}
