package com.vcc.template.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtil {
    private final MessageSource messageSource;

    public String getMessage(String key, Object[] value) {
        return messageSource.getMessage(key, value, LocaleContextHolder.getLocale());
    }
}
