package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkloadConfigRepository extends JpaRepository<WorkloadConfig, Long> {

    Optional<WorkloadConfig> findByCadreAndDeletedAtIsNull(Cadre cadre);
}
