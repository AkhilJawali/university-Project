package com.utms.masterdata.program;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.department.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "programs", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Program extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "duration_years", nullable = false)
    private Integer durationYears;

    @Column(name = "total_semesters", nullable = false)
    private Integer totalSemesters;

    @Enumerated(EnumType.STRING)
    @Column(name = "degree_type", nullable = false, length = 20)
    private DegreeType degreeType;
}
