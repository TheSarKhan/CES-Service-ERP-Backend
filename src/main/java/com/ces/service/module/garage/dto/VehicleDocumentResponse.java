package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.VehicleDocument;
import java.time.Instant;
import java.time.LocalDate;
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
public class VehicleDocumentResponse {

    private UUID id;
    private UUID vehicleId;
    private String docType;
    private String docNumber;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String notes;
    private Instant createdAt;

    public static VehicleDocumentResponse from(VehicleDocument d) {
        return VehicleDocumentResponse.builder()
                .id(d.getId())
                .vehicleId(d.getVehicleId())
                .docType(d.getDocType())
                .docNumber(d.getDocNumber())
                .issuedAt(d.getIssuedAt())
                .expiresAt(d.getExpiresAt())
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .fileSize(d.getFileSize())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
