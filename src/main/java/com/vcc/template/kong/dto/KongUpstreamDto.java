package com.vcc.template.kong.dto;

import com.vcc.template.kong.entity.SyncStatus;
import lombok.Data;

@Data
public class KongUpstreamDto {
    private Long id;
    private String kongId;
    private String name;
    private String displayName;
    private String algorithm;
    private Integer slots;
    private SyncStatus syncStatus;
    private java.util.Map<String, Object> healthchecks;
    private String hashOn;
    private String hashFallback;
    private String hashOnHeader;
    private String hashFallbackHeader;
    private String hashOnCookie;
    private String hashOnCookiePath;
    private String hashOnQueryArg;
    private String hashFallbackQueryArg;
    private String hashOnUriCapture;
    private String hashFallbackUriCapture;
    private String hostHeader;
    private String clientCertificate;
    private Boolean useSrvName;
    private java.util.List<String> tags;
}
