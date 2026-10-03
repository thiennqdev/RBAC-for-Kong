package com.vcc.template.common.util;

import com.vcc.template.common.dto.BaseSearchRequest;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@UtilityClass
public class RequestUtil {
    private static final int ASCENDING_SORT_VALUE = 1;

    public static Pageable getPageable(BaseSearchRequest criteria) {
        if (isSortingNotSpecified(criteria)) {
            return PageRequest.of(criteria.getPage(), criteria.getSize());
        }
        return PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(createSortOrders(criteria))
        );
    }

    private static boolean isSortingNotSpecified(BaseSearchRequest criteria) {
        return Objects.isNull(criteria.getSortField())
                || Objects.isNull(criteria.getSortOrder())
                || criteria.getSortField().isEmpty()
                || criteria.getSortOrder().isEmpty();
    }

    private static List<Sort.Order> createSortOrders(BaseSearchRequest criteria) {
        List<String> sortFields = criteria.getSortField();
        List<Integer> sortOrders = criteria.getSortOrder();
        int size = Math.min(sortFields.size(), sortOrders.size());

        return IntStream.range(0, size)
                .filter(i -> sortFields.get(i) != null)
                .mapToObj(i -> new Sort.Order(
                        determineSortDirection(sortOrders.get(i)),
                        sortFields.get(i)
                ))
                .toList();
    }

    private static Sort.Direction determineSortDirection(Integer sortValue) {
        return sortValue == ASCENDING_SORT_VALUE ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}