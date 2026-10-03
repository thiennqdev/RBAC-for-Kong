package com.vcc.template.kong.dto;

import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.entity.RouteStatus;
import lombok.Data;
import java.util.List;

@Data
public class KongRouteDto {
    private Long id;
    private String kongId;
    private Long kongServiceId;
    private String name;
    private String displayName;
    private List<String> paths;
    private List<String> methods;
    private List<String> protocols;
    private Boolean stripPath;
    private Boolean preserveHost;
    private SyncStatus syncStatus;
    private RouteStatus status;
    private List<String> hosts;
    private java.util.Map<String, List<String>> headers;
    private Integer httpsRedirectStatus;
    private Integer regexPriority;
    private String pathHandling;
    private Boolean requestBuffering;
    private Boolean responseBuffering;
    private List<String> snis;
    private List<java.util.Map<String, Object>> sources;
    private List<java.util.Map<String, Object>> destinations;
    private List<String> tags;
}
