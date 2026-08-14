package com.utms.masterdata.faculty;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface FacultyMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    FacultyDto toDto(Faculty faculty);

    @Mapping(target = "department", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Faculty toEntity(CreateFacultyRequest request);

    @Mapping(target = "department", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(CreateFacultyRequest request, @MappingTarget Faculty faculty);

    @Mapping(source = "faculty.id", target = "facultyId")
    FacultyAvailabilityDto toAvailabilityDto(FacultyAvailability availability);
}
