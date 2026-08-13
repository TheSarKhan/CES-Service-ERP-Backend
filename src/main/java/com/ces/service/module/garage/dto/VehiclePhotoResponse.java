package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.VehiclePhoto;
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
public class VehiclePhotoResponse {

    private UUID id;
    private UUID vehicleId;
    private String category;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String notes;
    private Instant createdAt;

    public static VehiclePhotoResponse from(VehiclePhoto p) {
        return VehiclePhotoResponse.builder()
                .id(p.getId())
                .vehicleId(p.getVehicleId())
                .category(p.getCategory())
                .fileName(p.getFileName())
                .fileUrl(p.getFileUrl())
                .fileSize(p.getFileSize())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
