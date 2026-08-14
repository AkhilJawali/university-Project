package com.utms.masterdata.batch;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface BatchMapper {

    @Mapping(source = "program.id", target = "programId")
    @Mapping(source = "program.name", target = "programName")
    BatchDto toDto(Batch batch);

    @Mapping(target = "program", ignore = true)
    Batch toEntity(CreateBatchRequest request);

    @Mapping(target = "program", ignore = true)
    void updateEntity(CreateBatchRequest request, @MappingTarget Batch batch);
}
