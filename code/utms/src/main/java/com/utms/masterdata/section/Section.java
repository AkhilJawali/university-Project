package com.utms.masterdata.section;

import com.utms.common.entity.BaseEntity;
import com.utms.masterdata.batch.Batch;
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
@Table(name = "sections", schema = "utms")
@Getter
@Setter
@NoArgsConstructor
public class Section extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "strength", nullable = false)
    private Integer strength;
}
