package com.vcc.template.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoragePolicyUpdateRequest {
    private List<StoragePolicyRuleDto> rules;
}
