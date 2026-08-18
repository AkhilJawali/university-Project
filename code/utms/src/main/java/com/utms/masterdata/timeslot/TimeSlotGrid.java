package com.utms.masterdata.timeslot;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.campus.Campus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "time_slot_grids", schema = "utms")
@Getter
@Setter
public class TimeSlotGrid extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @OneToMany(mappedBy = "grid", fetch = FetchType.LAZY)
    private List<SlotDefinition> slots = new ArrayList<>();

    @OneToMany(mappedBy = "grid", fetch = FetchType.LAZY)
    private List<WorkingDay> workingDays = new ArrayList<>();
}
