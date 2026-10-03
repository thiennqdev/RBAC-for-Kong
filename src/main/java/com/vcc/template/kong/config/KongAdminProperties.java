package com.vcc.template.kong.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kong.admin")
@Getter
@Setter
public class KongAdminProperties {
    private String url = "http://localhost:8001";
    private int timeout = 10000;
}
