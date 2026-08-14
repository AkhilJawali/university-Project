package com.utms.masterdata.program;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface ProgramMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    ProgramDto toDto(Program program);

    @Mapping(target = "department", ignore = true)
    Program toEntity(CreateProgramRequest request);

    @Mapping(target = "department", ignore = true)
    void updateEntity(CreateProgramRequest request, @MappingTarget Program program);
}
