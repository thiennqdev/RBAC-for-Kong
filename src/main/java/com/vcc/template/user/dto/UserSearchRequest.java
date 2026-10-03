package com.vcc.template.user.dto;

import com.vcc.template.common.dto.BaseSearchRequest;
import com.vcc.template.user.entity.UserStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Search request for users with keyword and status filter.
 */
@Getter
@Setter
public class UserSearchRequest extends BaseSearchRequest {
    private String keyword;
    private UserStatus status;
}
