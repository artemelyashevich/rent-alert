package com.rentalert.common.util.jpa;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtil {

    private SpecificationUtil() {
    }

    public static <T> Specification<T> buildLikeSpecification(String keyword, List<String> fields) {
        return (root, query, cb) -> {
            if (isBlank(keyword) || fields == null || fields.isEmpty()) {
                return cb.conjunction();
            }
            String pattern = likePattern(keyword);

            List<Predicate> predicates = new ArrayList<>();
            for (String field : fields) {
                Expression<String> path = resolveStringPath(root, field);
                predicates.add(cb.like(cb.lower(path), pattern));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T, E extends Enum<E>> Specification<T> buildSearchSpecification(
        String keyword,
        List<String> textFields
    ) {
        return buildSearchSpecification(keyword, textFields, null, null, null);
    }

    public static <T, E extends Enum<E>> Specification<T> buildSearchSpecification(
        String keyword,
        List<String> textFields,
        String numericField
    ) {
        return buildSearchSpecification(keyword, textFields, numericField, null, null);
    }

    public static <T, E extends Enum<E>> Specification<T> buildSearchSpecification(
        String keyword,
        List<String> textFields,
        String numericField,
        String enumField,
        Class<E> enumClass
    ) {
        return (root, query, cb) -> {
            if (isBlank(keyword)) {
                return cb.conjunction();
            }
            String trimmed = keyword.trim();
            String pattern = likePattern(trimmed);

            List<Predicate> predicates = new ArrayList<>();

            if (textFields != null) {
                for (String field : textFields) {
                    Expression<String> path = resolveStringPath(root, field);
                    predicates.add(cb.like(cb.lower(path), pattern));
                }
            }

            if (numericField != null) {
                try {
                    BigDecimal numericValue = new BigDecimal(trimmed);
                    Path<BigDecimal> numPath = root.get(numericField);
                    predicates.add(cb.equal(numPath, numericValue));
                } catch (NumberFormatException ignored) {
                    // keyword не является числом — пропускаем поиск по numericField
                }
            }

            if (enumField != null && enumClass != null) {
                for (E constant : enumClass.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(trimmed)) {
                        predicates.add(cb.equal(root.get(enumField), constant));
                        break;
                    }
                }
            }

            if (predicates.isEmpty()) {
                return cb.disjunction();
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String likePattern(String keyword) {
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    @SuppressWarnings("unchecked")
    private static <T> Expression<String> resolveStringPath(Root<T> root, String field) {
        Path<Object> path = root.get(field);
        if (String.class.equals(path.getJavaType())) {
            return (Expression<String>) (Expression<?>) path;
        }
        return path.as(String.class);
    }
}