package com.utms.masterdata.course;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface CourseMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    CourseDto toDto(Course course);

    @Mapping(target = "department", ignore = true)
    Course toEntity(CreateCourseRequest request);

    @Mapping(target = "department", ignore = true)
    void updateEntity(CreateCourseRequest request, @MappingTarget Course course);
}
