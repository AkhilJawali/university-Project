package com.utms.masterdata.academiccalendar;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface SpecialPeriodMapper {

    @Mapping(target = "calendarId", source = "calendar.id")
    SpecialPeriodDto toDto(SpecialPeriod specialPeriod);

    @Mapping(target = "calendar", ignore = true)
    SpecialPeriod toEntity(CreateSpecialPeriodRequest request);
}
