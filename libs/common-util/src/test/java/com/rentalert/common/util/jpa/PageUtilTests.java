package com.rentalert.common.util.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageUtilTests {

    @Test
    void copiesPageMetadata() {
        PageResponse<String> response = PageUtil.toResponse(new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 7));

        assertThat(response).isEqualTo(new PageResponse<>(List.of("a", "b"), 1, 2, 7, 4));
    }

    @Test
    void sortBecomesQueryParams() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("provider"), Sort.Order.desc("name")));

        assertThat(PageUtil.toSortParams(pageable)).containsExactly("provider,ASC", "name,DESC");
    }
}
