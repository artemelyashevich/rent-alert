package com.rentalert.core_ms.dto;

import java.util.List;
import lombok.Data;

@Data
public class RealtParams {

    private String operationName;
    private Variables variables;
    private String query;

    @Data
    public static class Variables {
        private DataInput data;
    }

    @Data
    public static class DataInput {
        private Where where;
        private Pagination pagination;
        private List<Sort> sort;
        private ExtraFields extraFields;
        private Boolean isReactAdaptiveUA;
    }

    @Data
    public static class Where {
        private String priceFrom;
        private String priceTo;
        private String priceType;
        private List<AddressV2> addressV2;
        private Integer category;
    }

    @Data
    public static class AddressV2 {
        private String townUuid;
    }

    @Data
    public static class Pagination {
        private Integer page;
        private Integer pageSize;
    }

    @Data
    public static class Sort {
        private String by;
        private String order;
    }

    @Data
    public static class ExtraFields {
        private String minPriceAggregation;
    }
}