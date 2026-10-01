package com.rentalert.common.util.jpa;

import com.rentalert.dto.PageResponse;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UtilityClass
public class PageUtil {

    public static <T> PageResponse<T> toResponse(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static List<String> toSortParams(Pageable pageable) {
        return pageable.getSort().stream()
            .map(order -> order.getProperty() + "," + order.getDirection().name())
            .toList();
    }
}
