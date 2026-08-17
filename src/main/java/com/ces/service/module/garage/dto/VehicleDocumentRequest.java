package com.ces.service.module.garage.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Metadata alongside a document upload — the file itself travels as a separate multipart part. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDocumentRequest {

    @NotBlank
    private String docType;

    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private String notes;
}
