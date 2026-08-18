package com.utms.masterdata.academiccalendar;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_calendars", schema = "utms")
@Getter
@Setter
public class AcademicCalendar extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester_type", nullable = false, length = 10)
    private SemesterType semesterType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<Holiday> holidays = new ArrayList<>();

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<ExamWindow> examWindows = new ArrayList<>();

    @OneToMany(mappedBy = "calendar", fetch = FetchType.LAZY)
    private List<SpecialPeriod> specialPeriods = new ArrayList<>();
}
