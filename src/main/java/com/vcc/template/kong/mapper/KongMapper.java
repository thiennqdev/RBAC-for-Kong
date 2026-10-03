package com.vcc.template.kong.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcc.template.kong.dto.*;
import com.vcc.template.kong.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

@Mapper
public interface KongMapper {
    KongMapper INSTANCE = Mappers.getMapper(KongMapper.class);
    
    ObjectMapper jsonMapper = new ObjectMapper();

    @Mapping(target = "tags", source = "tags", qualifiedByName = "jsonToList")
    KongServiceDto toDto(KongServiceEntity entity);

    @Mapping(target = "kongServiceId", source = "kongService.id")
    @Mapping(target = "paths", source = "paths", qualifiedByName = "jsonToList")
    @Mapping(target = "methods", source = "methods", qualifiedByName = "jsonToList")
    @Mapping(target = "protocols", source = "protocols", qualifiedByName = "jsonToList")
    KongRouteDto toDto(KongRouteEntity entity);

    KongUpstreamDto toDto(KongUpstreamEntity entity);

    @Mapping(target = "kongUpstreamId", source = "kongUpstream.id")
    KongTargetDto toDto(KongTargetEntity entity);

    @Mapping(target = "config", source = "config", qualifiedByName = "jsonToMap")
    KongPluginDto toDto(KongPluginEntity entity);

    @Named("jsonToList")
    default List<String> jsonToList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return jsonMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("listToJson")
    default String listToJson(List<String> list) {
        if (list == null) return null;
        try {
            return jsonMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
    
    @Named("jsonToMap")
    default Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("mapToJson")
    default String mapToJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return jsonMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
