package com.vcc.template.access.service;

import com.vcc.template.access.dto.AssignMemberRequest;
import com.vcc.template.access.dto.BatchAssignMemberRequest;
import com.vcc.template.access.dto.ProjectMemberResponse;
import com.vcc.template.access.dto.UpdateMemberRoleRequest;

import java.util.List;

/**
 * Service interface for managing project member access.
 */
public interface ProjectMemberService {

    ProjectMemberResponse assignMember(String projectCode, AssignMemberRequest request);

    List<ProjectMemberResponse> batchAssignMembers(String projectCode, BatchAssignMemberRequest request);

    List<ProjectMemberResponse> listMembers(String projectCode);

    ProjectMemberResponse updateMemberRole(String projectCode, Long userId, UpdateMemberRoleRequest request);

    void removeMember(String projectCode, Long userId);
}
