package com.vcc.template.common.exception.errorcode;

import com.vcc.template.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    String getCode();
    HttpStatus getStatus();

    default BusinessException toException(Object... args) {
        return new BusinessException(this, args);
    }
}
