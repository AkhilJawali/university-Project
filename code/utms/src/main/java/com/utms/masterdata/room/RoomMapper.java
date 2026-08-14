package com.utms.masterdata.room;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", config = com.utms.common.mapper.BaseMapperConfig.class)
public interface RoomMapper {

    @Mapping(source = "campus.id", target = "campusId")
    @Mapping(source = "campus.name", target = "campusName")
    RoomDto toDto(Room room);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Room toEntity(CreateRoomRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "campus", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(CreateRoomRequest request, @MappingTarget Room room);
}
