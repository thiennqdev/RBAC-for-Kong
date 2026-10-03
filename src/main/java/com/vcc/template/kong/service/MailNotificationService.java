package com.vcc.template.kong.service;

import com.vcc.template.access.model.ProjectRole;
import com.vcc.template.access.repository.UserProjectRoleRepository;
import com.vcc.template.kong.entity.KongRouteEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailNotificationService {

    private final JavaMailSender javaMailSender;
    private final UserProjectRoleRepository userProjectRoleRepository;

    @Value("${app.mail.from:noreply@vcc.com}")
    private String mailFrom;

    public void sendRouteApprovalRequest(Long kongInstanceId, String projectCode, KongRouteEntity route) {
        try {
            List<String> adminEmails = userProjectRoleRepository
                    .findByProjectProjectCodeAndRoleName(projectCode, ProjectRole.PROJECT_ADMIN)
                    .stream()
                    .map(upr -> upr.getUser().getEmail())
                    .filter(email -> email != null && !email.isBlank())
                    .collect(Collectors.toList());

            if (adminEmails.isEmpty()) {
                log.warn("No PROJECT_ADMIN emails found for project {}", projectCode);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(adminEmails.toArray(new String[0]));
            message.setSubject("[RBAC for Kong] Route Approval Request");
            message.setText(String.format("A new route '%s' in project '%s' has been created/updated and requires your approval.\n\n" +
                    "Please log in to the dashboard to review and approve/reject this route.",
                    route.getDisplayName(), projectCode));

            javaMailSender.send(message);
            log.info("Sent route approval request email to {} admins for project {}", adminEmails.size(), projectCode);
        } catch (Exception e) {
            log.error("Failed to send route approval request email", e);
            // Do not throw exception so we don't break the main transaction flow
        }
    }
}
