package com.alphnology.utils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import static com.alphnology.utils.CommonUtils.normalizeText;

/**
 * @author me@fredpena.dev
 * @created 28/11/2024  - 17:42
 */
public final class PredicateUtils {
    
    private static final String UNACCENT = "unaccent";

    private PredicateUtils() {
    }


    public static Predicate predicateUnaccentLike(CriteriaBuilder builder, Path<?> path, String field, String search) {
        return builder.like(builder.function(UNACCENT, String.class, builder.lower(path.get(field))), "%" + normalizeText(search) + "%");
    }

    public static <T> Predicate predicateUnaccentLike(Root<T> root, CriteriaBuilder builder, String field, String filter) {

        return builder.like(builder.function(UNACCENT, String.class, builder.lower(root.get(field))), "%" + normalizeText(filter) + "%");
    }

    public static <T> Predicate predicateUnaccentLike(Path<T> path, CriteriaBuilder builder, String field, String filter) {

        return builder.like(builder.function(UNACCENT, String.class, builder.lower(path.get(field))), "%" + normalizeText(filter) + "%");
    }

    public static Predicate createPredicateForSelectedItems(Optional<Collection<?>> selectedItems, Function<Collection<?>, Predicate> predicateFunction, CriteriaBuilder builder) {
        return selectedItems.filter(items -> !items.isEmpty()).map(predicateFunction).orElseGet(builder::conjunction);
    }

    public static Predicate createPredicateForDateTimeRange(LocalDateTime initDate, LocalDateTime endDate, Path<LocalDateTime> pathGreater, Path<LocalDateTime> pathLess, CriteriaBuilder builder) {
        if (initDate != null && endDate != null) {
            return builder.and(builder.greaterThanOrEqualTo(pathGreater, initDate), builder.lessThanOrEqualTo(pathLess, endDate));
        }
        return builder.conjunction();
    }

}
