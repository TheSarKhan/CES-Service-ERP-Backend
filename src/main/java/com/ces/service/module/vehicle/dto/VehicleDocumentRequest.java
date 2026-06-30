package com.ces.service.module.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload for attaching a document to a vehicle (POST /vehicles/{id}/documents). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDocumentRequest {

    @NotBlank
    @Size(max = 100)
    private String docType;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    private String fileUrl;

    private Long fileSize;

    private LocalDate expiresAt;
}
