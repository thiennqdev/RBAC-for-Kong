package com.vcc.template.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoragePolicyRuleDto {
    private String prefix;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private List<String> ipAddresses;
}
