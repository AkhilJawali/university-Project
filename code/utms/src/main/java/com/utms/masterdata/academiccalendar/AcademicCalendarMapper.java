package com.utms.masterdata.academiccalendar;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface AcademicCalendarMapper {

    @Mapping(target = "campusId", source = "campus.id")
    @Mapping(target = "campusName", source = "campus.name")
    @Mapping(target = "holidayCount", ignore = true)
    @Mapping(target = "examWindowCount", ignore = true)
    @Mapping(target = "specialPeriodCount", ignore = true)
    AcademicCalendarDto toDto(AcademicCalendar calendar);

    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "holidays", ignore = true)
    @Mapping(target = "examWindows", ignore = true)
    @Mapping(target = "specialPeriods", ignore = true)
    AcademicCalendar toEntity(CreateAcademicCalendarRequest request);
}
