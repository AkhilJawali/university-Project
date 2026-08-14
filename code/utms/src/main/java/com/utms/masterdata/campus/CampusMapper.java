package com.utms.masterdata.campus;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface CampusMapper {

    CampusDto toDto(Campus campus);

    Campus toEntity(CreateCampusRequest request);

    void updateEntity(UpdateCampusRequest request, @MappingTarget Campus campus);
}
