package com.utms.masterdata.section;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface SectionMapper {

    @Mapping(source = "batch.id", target = "batchId")
    @Mapping(source = "batch.name", target = "batchName")
    SectionDto toDto(Section section);

    @Mapping(target = "batch", ignore = true)
    Section toEntity(CreateSectionRequest request);

    @Mapping(target = "batch", ignore = true)
    void updateEntity(CreateSectionRequest request, @MappingTarget Section section);
}
