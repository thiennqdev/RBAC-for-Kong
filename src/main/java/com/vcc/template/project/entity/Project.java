package com.vcc.template.project.entity;

import com.vcc.template.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a project in the multi-tenant RBAC system.
 * Each project corresponds to a single tenant with its own isolated database schema.
 */
@Entity
@Table(schema = "public", name = "projects", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_code", columnNames = "project_code")
})
@Getter
@Setter
public class Project extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique, immutable identifier used as both the tenant identifier and PostgreSQL schema name.
     * Must be 3-50 alphanumeric characters or hyphens.
     */
    @Column(name = "project_code", nullable = false, updatable = false, length = 50)
    private String projectCode;

    /**
     * Human-readable name of the project.
     * Maximum 200 characters.
     */
    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    /**
     * Path to the project icon in object storage (MinIO/S3).
     * Format: {category}/{uuid}.{ext}
     * Maximum 500 characters.
     */
    @Column(name = "icon_path", length = 500)
    private String iconPath;

    /**
     * Current status of the project.
     * ACTIVE: Project is operational and accessible.
     * INACTIVE: Project is soft-deleted and excluded from tenant resolution.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    /**
     * Identity Provider type for this project's API authentication.
     * DEFAULT: Uses the system's shared Keycloak realm.
     * CUSTOM_KEYCLOAK, AUTH0, CUSTOM_OIDC: Uses a project-specific IdP.
     */
    @Column(name = "idp_type", nullable = false, length = 30)
    private String idpType = "DEFAULT";

    /**
     * Custom IdP issuer URL (e.g. https://auth.company.com/realms/my-realm).
     * Only used when idpType is not DEFAULT.
     */
    @Column(name = "idp_issuer_url", length = 500)
    private String idpIssuerUrl;

    /**
     * Custom IdP JWKS endpoint URL for fetching public keys.
     * Only used when idpType is not DEFAULT.
     */
    @Column(name = "idp_jwks_url", length = 500)
    private String idpJwksUrl;

    /**
     * OIDC Client ID for Kong authentication.
     */
    @Column(name = "idp_client_id", length = 200)
    private String idpClientId;

    /**
     * OIDC Client Secret for Kong authentication.
     */
    @Column(name = "idp_client_secret", length = 500)
    private String idpClientSecret;

    /**
     * Whether JWT authentication should be enabled on the Kong Gateway for this project.
     * If false, Kong will not enforce JWT for the services of this project.
     */
    @Column(name = "jwt_auth_enabled", nullable = false)
    private Boolean jwtAuthEnabled = true;
}
