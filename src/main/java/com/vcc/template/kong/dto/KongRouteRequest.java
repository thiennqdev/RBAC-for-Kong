package com.vcc.template.kong.dto;

import lombok.Data;
import java.util.List;

@Data
public class KongRouteRequest {
    private String name;
    private List<String> paths;
    private List<String> methods;
    private List<String> protocols;
    private Boolean stripPath = true;
    private Boolean preserveHost = false;
    private List<String> hosts;
    private java.util.Map<String, List<String>> headers;
    private Integer httpsRedirectStatus = 426;
    private Integer regexPriority = 0;
    private String pathHandling = "v0";
    private Boolean requestBuffering = true;
    private Boolean responseBuffering = true;
    private List<String> snis;
    private List<java.util.Map<String, Object>> sources;
    private List<java.util.Map<String, Object>> destinations;
    private List<String> tags;
}
