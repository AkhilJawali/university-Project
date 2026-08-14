package com.utms.masterdata.course;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Course extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "lecture_hours", nullable = false)
    private Integer lectureHours;

    @Column(name = "tutorial_hours", nullable = false)
    private Integer tutorialHours;

    @Column(name = "practical_hours", nullable = false)
    private Integer practicalHours;

    @Column(name = "credit_hours", nullable = false)
    private Integer creditHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType;

    @Column(name = "is_cross_listed", nullable = false)
    private Boolean isCrossListed = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prerequisites", columnDefinition = "jsonb")
    private List<Long> prerequisites = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equipment_tags", columnDefinition = "jsonb")
    private List<String> equipmentTags = new ArrayList<>();
}
