package com.utms.masterdata.department;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface DepartmentMapper {

    @Mapping(source = "campus.id", target = "campusId")
    @Mapping(source = "campus.name", target = "campusName")
    DepartmentDto toDto(Department department);

    @Mapping(target = "campus", ignore = true)
    Department toEntity(CreateDepartmentRequest request);

    @Mapping(target = "campus", ignore = true)
    void updateEntity(CreateDepartmentRequest request, @MappingTarget Department department);
}
