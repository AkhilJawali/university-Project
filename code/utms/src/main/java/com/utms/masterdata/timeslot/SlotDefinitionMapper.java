package com.utms.masterdata.timeslot;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", config = BaseMapperConfig.class)
public interface SlotDefinitionMapper {

    @Mapping(target = "gridId", source = "grid.id")
    SlotDefinitionDto toDto(SlotDefinition slotDefinition);

    @Mapping(target = "grid", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    SlotDefinition toEntity(CreateSlotDefinitionRequest request);
}
