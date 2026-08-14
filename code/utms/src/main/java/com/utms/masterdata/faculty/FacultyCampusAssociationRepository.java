package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyCampusAssociationRepository extends JpaRepository<FacultyCampusAssociation, Long> {

    List<FacultyCampusAssociation> findAllByFacultyIdAndDeletedAtIsNull(Long facultyId);

    Optional<FacultyCampusAssociation> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByFacultyIdAndCampusIdAndDeletedAtIsNull(Long facultyId, Long campusId);
}
