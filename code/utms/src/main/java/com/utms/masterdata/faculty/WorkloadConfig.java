package com.utms.masterdata.faculty;

import com.utms.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workload_configs", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class WorkloadConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "cadre", nullable = false, unique = true, length = 30)
    private Cadre cadre;

    @Column(name = "min_weekly_hours", nullable = false)
    private Integer minWeeklyHours;

    @Column(name = "max_weekly_hours", nullable = false)
    private Integer maxWeeklyHours;
}
