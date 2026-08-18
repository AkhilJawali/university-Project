package com.utms.masterdata.academiccalendar;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface HolidayMapper {

    @Mapping(target = "calendarId", source = "calendar.id")
    HolidayDto toDto(Holiday holiday);

    @Mapping(target = "calendar", ignore = true)
    Holiday toEntity(CreateHolidayRequest request);
}
