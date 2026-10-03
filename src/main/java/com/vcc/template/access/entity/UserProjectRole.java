package com.vcc.template.access.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import com.vcc.template.project.entity.Project;
import com.vcc.template.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.vcc.template.access.model.ProjectRole;

/**
 * Entity representing the assignment of a user to a project with a specific
 * role.
 * Each user can have only one role per project (enforced by unique constraint).
 * Stored in the global (public) schema.
 * 
 * Note: role_name is stored directly as an Enum instead of a foreign key
 * to simplify the RBAC system and decouple it from dynamic roles.
 */
@Entity
@Table(schema = "public", name = "user_project_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_project_roles_user_project", columnNames = { "user_id", "project_id" })
})
@Getter
@Setter
public class UserProjectRole extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * Stores the user's role directly.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 50)
    private ProjectRole roleName;
}

