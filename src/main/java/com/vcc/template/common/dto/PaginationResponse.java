package com.vcc.template.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponse<T> {
    private long totalElements;
    private int totalPages;
    private List<T> data;

}
