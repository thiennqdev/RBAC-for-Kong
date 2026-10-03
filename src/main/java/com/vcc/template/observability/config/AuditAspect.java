package com.vcc.template.observability.config;

import com.vcc.template.observability.entity.AuditLog;
import com.vcc.template.observability.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.vcc.template.kong.service.*.create*(..)) || " +
            "execution(* com.vcc.template.kong.service.*.update*(..)) || " +
            "execution(* com.vcc.template.kong.service.*.add*(..)) || " +
            "execution(* com.vcc.template.kong.service.*.delete*(..))")
    public Object auditKongOperations(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String action = (methodName.startsWith("create") || methodName.startsWith("add")) ? "CREATE" :
                        methodName.startsWith("update") ? "UPDATE" : "DELETE";

        String entityName = pjp.getTarget().getClass().getSimpleName();
        if (entityName.endsWith("ServiceImpl")) {
            entityName = entityName.substring(0, entityName.length() - 11);
        } else if (entityName.endsWith("Service")) {
            entityName = entityName.substring(0, entityName.length() - 7);
        }
        if (entityName.startsWith("Kong")) {
            entityName = entityName.substring(4); // Remove Kong prefix
        }
        entityName = entityName.toUpperCase();

        // Find projectCode from TenantContext since it's already extracted from the URI
        String projectCode = com.vcc.template.common.tenant.TenantContext.getCurrentTenant();
        if (projectCode == null) {
            projectCode = "unknown";
        }
        Object[] args = pjp.getArgs();

        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                // Determine Actor
                String actor = "system";
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof com.vcc.template.common.security.KongTrustedAuthenticationToken kongAuth) {
                    actor = kongAuth.getClaimAsString("preferred_username");
                    if (actor == null) actor = kongAuth.getSubject();
                } else if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                    actor = jwt.getClaimAsString("preferred_username");
                    if (actor == null) actor = jwt.getSubject();
                }

                // Determine IP
                String ipAddress = "";
                ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (sra != null) {
                    HttpServletRequest request = sra.getRequest();
                    ipAddress = request.getRemoteAddr();
                }

                String entityId = "unknown";
                if (action.equals("DELETE") && args.length > 0) {
                    entityId = String.valueOf(args[args.length - 1]);
                } else if (result != null) {
                    try {
                        // Attempt to extract 'getId' or 'getKongId' from response DTO
                        java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
                        Object idVal = getIdMethod.invoke(result);
                        if (idVal != null) entityId = String.valueOf(idVal);
                    } catch (Exception e) {
                        try {
                            java.lang.reflect.Method getKongIdMethod = result.getClass().getMethod("getKongId");
                            Object kongIdVal = getKongIdMethod.invoke(result);
                            if (kongIdVal != null) entityId = String.valueOf(kongIdVal);
                        } catch (Exception ex) {
                            // Ignored
                        }
                    }
                }

                AuditLog auditLog = AuditLog.builder()
                        .projectCode(projectCode)
                        .action(action)
                        .entityName(entityName)
                        .entityId(entityId)
                        .actor(actor)
                        .ipAddress(ipAddress)
                        .newValue(action.equals("DELETE") || error != null || result == null ? null : objectMapper.writeValueAsString(result))
                        .build();

                log.info("Attempting to save AuditLog: action={}, entityName={}, entityId={}", action, entityName, entityId);
                // tenant schema should already be set by the Controller/Filter
                auditLogService.saveAuditLog(auditLog);
                log.info("Successfully saved AuditLog.");

            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
    }
}
