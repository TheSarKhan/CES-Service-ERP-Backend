package com.ces.service.module.enginehours.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.enginehours.enums.MeterType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * One append-only entry in a vehicle's meter history (Motosaat/KM — SRS M08). Readings are never
 * edited or deleted; a mistaken value is corrected by recording a rollover instead, which keeps
 * the row itself an honest record of what was actually entered and when.
 */
@Entity
@Table(name = "meter_readings", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MeterReading extends BaseEntity {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_type", nullable = false)
    private MeterType meterType;

    @Column(name = "value", nullable = false)
    private BigDecimal value;

    @Column(name = "previous_value")
    private BigDecimal previousValue;

    /** DB-computed (`value - previous_value`); never set from Java. */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "delta", insertable = false, updatable = false)
    private BigDecimal delta;

    /** Free text — validated against {@code garage_config_values(METER_SOURCE)}. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "is_rollover", nullable = false)
    @Builder.Default
    private Boolean isRollover = false;

    @Column(name = "rollover_reason")
    private String rolloverReason;

    /** Origin record when {@code source} points at another module (e.g. a maintenance completion). */
    @Column(name = "source_ref_id")
    private UUID sourceRefId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(name = "notes")
    private String notes;
}
