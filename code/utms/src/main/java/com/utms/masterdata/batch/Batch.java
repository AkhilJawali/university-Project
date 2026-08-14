package com.utms.masterdata.batch;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.program.Program;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "batches", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Batch extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(name = "strength", nullable = false)
    private Integer strength;
}
