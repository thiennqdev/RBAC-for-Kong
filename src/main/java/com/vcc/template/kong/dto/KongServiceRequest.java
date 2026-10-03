package com.vcc.template.kong.dto;

import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.NotBlank;

@Data
public class KongServiceRequest {
    @NotBlank
    private String name;
    private String host;
    private Integer port;
    private String protocol = "http";
    private String path;
    private Boolean enabled = true;
    private List<String> tags;
    private Integer retries = 5;
    private Integer connectTimeout = 60000;
    private Integer writeTimeout = 60000;
    private Integer readTimeout = 60000;
    private String clientCertificate;
    private Boolean tlsVerify;
    private Integer tlsVerifyDepth;
    private List<String> caCertificates;
}
