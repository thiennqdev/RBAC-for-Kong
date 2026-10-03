package com.vcc.template.kong.dto;

import com.vcc.template.kong.entity.SyncStatus;
import lombok.Data;
import java.util.List;

@Data
public class KongServiceDto {
    private Long id;
    private String kongId;
    private String name;
    private String displayName;
    private String host;
    private Integer port;
    private String protocol;
    private String path;
    private Boolean enabled;
    private SyncStatus syncStatus;
    private List<String> tags;
    private Integer retries;
    private Integer connectTimeout;
    private Integer writeTimeout;
    private Integer readTimeout;
    private String clientCertificate;
    private Boolean tlsVerify;
    private Integer tlsVerifyDepth;
    private List<String> caCertificates;
}
