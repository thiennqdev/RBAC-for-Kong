package com.vcc.template.project.mapper;

import com.vcc.template.common.config.MapStructConfig;
import com.vcc.template.project.dto.CreateProjectRequest;
import com.vcc.template.project.dto.ProjectResponse;
import com.vcc.template.project.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Project entity and DTOs.
 */
@Mapper(config = MapStructConfig.class)
public interface ProjectMapper {

    /**
     * Map Project entity to ProjectResponse DTO.
     *
     * @param project the project entity
     * @return ProjectResponse DTO with all project data
     */
    ProjectResponse toResponse(Project project);

    /**
     * Map CreateProjectRequest to Project entity.
     * Note: Status and audit fields are set by the service layer.
     *
     * @param request the creation request
     * @return Project entity with basic fields populated
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Project toEntity(CreateProjectRequest request);
}
