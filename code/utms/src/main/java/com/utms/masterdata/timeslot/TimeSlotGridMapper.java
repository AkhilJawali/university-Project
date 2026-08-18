package com.utms.masterdata.timeslot;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.DayOfWeek;
import java.util.List;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface TimeSlotGridMapper {

    @Mapping(target = "campusId", source = "campus.id")
    @Mapping(target = "campusName", source = "campus.name")
    @Mapping(target = "slots", source = "slots")
    @Mapping(target = "workingDays", source = "workingDays")
    TimeSlotGridDto toDto(TimeSlotGrid grid);

    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "workingDays", ignore = true)
    TimeSlotGrid toEntity(CreateTimeSlotGridRequest request);

    default WorkingDayDto workingDayToDto(WorkingDay workingDay) {
        return WorkingDayDto.builder()
                .dayOfWeek(workingDay.getDayOfWeek())
                .dayName(DayOfWeek.of(workingDay.getDayOfWeek()).name())
                .isWorkingDay(workingDay.getIsWorkingDay())
                .build();
    }

    List<WorkingDayDto> workingDaysToDto(List<WorkingDay> workingDays);
}
