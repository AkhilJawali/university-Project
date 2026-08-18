package com.utms.masterdata.academiccalendar;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface ExamWindowMapper {

    @Mapping(target = "calendarId", source = "calendar.id")
    ExamWindowDto toDto(ExamWindow examWindow);

    @Mapping(target = "calendar", ignore = true)
    ExamWindow toEntity(CreateExamWindowRequest request);
}
