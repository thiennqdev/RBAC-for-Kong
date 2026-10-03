package com.vcc.template.common.exception;

import com.vcc.template.common.exception.errorcode.BaseErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final BaseErrorCode errorCode;
    private final Object[] args;

    public BusinessException(BaseErrorCode errorCode, Object... args) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.args = args;
    }
}