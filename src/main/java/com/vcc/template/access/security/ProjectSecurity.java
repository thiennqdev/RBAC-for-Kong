package com.vcc.template.access.security;

import com.vcc.template.access.entity.UserProjectRole;
import com.vcc.template.access.model.ProjectRole;
import com.vcc.template.access.repository.UserProjectRoleRepository;
import com.vcc.template.user.entity.User;
import com.vcc.template.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurity {

    private final UserRepository userRepository;
    private final UserProjectRoleRepository userProjectRoleRepository;

    @Transactional(readOnly = true)
    public boolean hasAnyRole(String projectCode, String... roleNames) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return false;
        }

        // SYSTEM_ADMIN overrides all project rules
        if (auth.getAuthorities().stream().anyMatch(a -> "SYSTEM_ADMIN".equals(a.getAuthority()))) {
            return true;
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        Optional<UserProjectRole> uprOpt = userProjectRoleRepository.findByUserIdAndProjectCode(userOpt.get().getId(), projectCode);
        if (uprOpt.isEmpty()) {
            return false;
        }

        String userRole = uprOpt.get().getRoleName().name();
        return Arrays.asList(roleNames).contains(userRole);
    }
    
    @Transactional(readOnly = true)
    public boolean hasRole(String projectCode, String roleName) {
        return hasAnyRole(projectCode, roleName);
    }
}
