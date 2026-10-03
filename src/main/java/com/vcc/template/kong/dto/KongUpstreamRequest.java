package com.vcc.template.kong.dto;

import lombok.Data;

@Data
public class KongUpstreamRequest {
    private String name;
    private String algorithm = "round-robin";
    private Integer slots = 10000;
    private java.util.Map<String, Object> healthchecks;
    private String hashOn = "none";
    private String hashFallback = "none";
    private String hashOnHeader;
    private String hashFallbackHeader;
    private String hashOnCookie;
    private String hashOnCookiePath = "/";
    private String hashOnQueryArg;
    private String hashFallbackQueryArg;
    private String hashOnUriCapture;
    private String hashFallbackUriCapture;
    private String hostHeader;
    private String clientCertificate;
    private Boolean useSrvName = false;
    private java.util.List<String> tags;
}
