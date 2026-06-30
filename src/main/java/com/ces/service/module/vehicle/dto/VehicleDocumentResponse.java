package com.ces.service.module.vehicle.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Vehicle document view. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDocumentResponse {

    private UUID id;
    private UUID vehicleId;
    private String docType;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private LocalDate expiresAt;
    private Instant createdAt;
}
