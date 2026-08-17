package com.ces.service.module.garage.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Branch-scoped Motosaat thresholds (SRS/Motosaat brief "Anormal motosaat artımı" / "Motosaat
 * yenilənməmə limiti") — one row per branch, all columns nullable so a branch that never visits
 * Konfiqurasiya just has the feature switched off rather than a silently-hardcoded default.
 */
@Entity
@Table(name = "garage_settings", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GarageSettings extends BaseEntity {

    /** Neçə gün ərzində yeni oxunuş qeyd olunmayıbsa, texnika "yenilənməmiş" sayılır. */
    @Column(name = "stale_reading_days")
    private Integer staleReadingDays;

    /** Bir qeyddə əvvəlki dəyərdən bu qədər artıq fərq varsa, "anormal artım" xəbərdarlığı göstərilir. */
    @Column(name = "max_normal_increase_engine_hours")
    private BigDecimal maxNormalIncreaseEngineHours;

    @Column(name = "max_normal_increase_km")
    private BigDecimal maxNormalIncreaseKm;
}
