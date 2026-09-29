package com.rentalert.processing_ms.util;

import com.rentalert.common.util.JsonHelper;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.dto.KufarParams;
import com.rentalert.processing_ms.dto.RealtParams;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ParserRequestUtil {

    private static final String USD_CODE = "840";
    private static final String BYN_CODE = "933";
    private static final String DEFAULT_CURRENCY = "BYN";

    public static Map<String, Object> buildOnlinerParams(SchedulerSettingsResponse settings, City city) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("currency", settings.getPriceCurrency() != null
            ? settings.getPriceCurrency().toUpperCase()
            : DEFAULT_CURRENCY);
        params.putAll(parseQueryFragment(city.getCityCode()));
        params.put("page", 1);
        params.put("order",
            (settings.getSortBy() != null ? settings.getSortBy() : "created_at")
                + ":"
                + (settings.getSortOrder() != null ? settings.getSortOrder().toLowerCase() : "desc"));
        params.put("v", Math.random());
        return params;
    }

    public static Map<String, Object> buildOnlinerRoomParams(SchedulerSettingsResponse settings, City city) {
        Map<String, Object> params = buildOnlinerParams(settings, city);
        params.put("room_type", 1);
        return params;
    }

    private static Map<String, Object> parseQueryFragment(String fragment) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        if (fragment == null || fragment.isBlank()) {
            return parsed;
        }
        String normalized = fragment.strip();
        if (normalized.startsWith("?") || normalized.startsWith("&")) {
            normalized = normalized.substring(1);
        }
        for (String pair : normalized.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            parsed.put(pair.substring(0, eq).strip(), pair.substring(eq + 1).strip());
        }
        return parsed;
    }

    private static final String REALT_SEARCH_QUERY = """
        query searchObjects($data: GetObjectsByAddressInput!) {
          searchObjects(data: $data) {
            body {
              results {
                uuid
                code
                headline
                description
                createdAt
                updatedAt
                metroTime
                metroStationName
                price
                priceCurrency
                storeys
                storey
                rooms
                contactPhones
                images
                areaTotal
                streetName
                address
                contactName
                furniture
                location
              }
              pagination {
                page
                pageSize
                totalCount
              }
            }
          }
        }
        """;

    private static final String REALT_ROOM_SEARCH_QUERY = """
        query searchObjects($data: GetObjectsByAddressInput!) {
          searchObjects(data: $data) {
            body {
              results {
                uuid
                code
                headline
                description
                createdAt
                updatedAt
                metroTime
                metroStationName
                price
                priceCurrency
                storeys
                storey
                rooms
                contactPhones
                images
                areaTotal
                areaLiving
                streetName
                address
                contactName
                furniture
                repairState
                appliances
                location
              }
              pagination {
                page
                pageSize
                totalCount
              }
            }
          }
        }
        """;

    public static RealtParams buildRealtParams(SchedulerSettingsResponse settings, City city) {
        return buildRealtParams(settings, city, REALT_SEARCH_QUERY, settings.getCategoryId());
    }

    public static RealtParams buildRealtRoomParams(SchedulerSettingsResponse settings, City city) {
        Integer roomCategoryId = settings.getRoomCategoryId() != null
            ? settings.getRoomCategoryId()
            : settings.getCategoryId();
        return buildRealtParams(settings, city, REALT_ROOM_SEARCH_QUERY, roomCategoryId);
    }

    private static RealtParams buildRealtParams(SchedulerSettingsResponse settings, City city, String query, Integer categoryId) {
        RealtParams params = new RealtParams();

        params.setOperationName("searchObjects");
        params.setQuery(query);

        RealtParams.Where where = new RealtParams.Where();
        where.setPriceFrom(settings.getPriceFrom() != null ? settings.getPriceFrom().toString() : "0");
        where.setPriceTo(settings.getPriceTo() != null ? settings.getPriceTo().toString() : "1000");
        where.setPriceType(getCurrencyCode(settings.getPriceCurrency()));
        where.setCategory(categoryId);

        RealtParams.AddressV2 address = new RealtParams.AddressV2();
        address.setTownUuid(city.getCityCode());

        where.setAddressV2(List.of(address));

        RealtParams.Pagination pagination = new RealtParams.Pagination();
        pagination.setPage(1);
        pagination.setPageSize(settings.getPageSize());

        RealtParams.Sort sort = new RealtParams.Sort();
        sort.setBy(settings.getSortBy());
        sort.setOrder(settings.getSortOrder());

        RealtParams.DataInput data = new RealtParams.DataInput();
        data.setWhere(where);
        data.setPagination(pagination);
        data.setSort(List.of(sort));
        data.setExtraFields(null);
        data.setIsReactAdaptiveUA(false);

        RealtParams.Variables variables = new RealtParams.Variables();
        variables.setData(data);

        params.setVariables(variables);
        return params;
    }

    public static Map<String, Object> buildKufarParams(SchedulerSettingsResponse settings, City city) {
        return buildKufarParams(settings, city, settings.getCategoryId());
    }

    public static Map<String, Object> buildKufarRoomParams(SchedulerSettingsResponse settings, City city) {
        Integer roomCategoryId = settings.getRoomCategoryId() != null
            ? settings.getRoomCategoryId()
            : settings.getCategoryId();
        return buildKufarParams(settings, city, roomCategoryId);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildKufarParams(SchedulerSettingsResponse settings, City city, Integer categoryId) {
        String priceRange = "r:" +
            (settings.getPriceFrom() != null ? settings.getPriceFrom() : 0) + "," +
            (settings.getPriceTo() != null ? settings.getPriceTo() : 1000);

        KufarParams params = KufarParams.builder()
            .price(priceRange)
            .currency(getCurrencyCode(settings.getPriceCurrency()))
            .category(categoryId.toString())
            .size(settings.getPageSize())
            .sort(settings.getSortBy())
            .build();

        String json = JsonHelper.toJson(params);
        Map<String, Object> result = (Map<String, Object>) JsonHelper.fromJson(json, Map.class);
        result.put("gtsy", city.getCityCode());
        return result;
    }

    private static String getCurrencyCode(String currency) {
        if (currency == null) {
            return BYN_CODE;
        }
        return switch (currency.toUpperCase()) {
            case "USD" -> USD_CODE;
            case "BYN" -> BYN_CODE;
            default -> BYN_CODE;
        };
    }
}
