package com.alphnology.utils;

import com.alphnology.data.Speaker;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author me@fredpena.dev
 * @created 15/06/2025  - 18:18
 */
public class RendererUtils {

    private RendererUtils() {
    }

    private static <T> String get(Set<T> items, Function<T, String> itemMapper) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(itemMapper)
                .collect(Collectors.joining(", "));
    }


    public static String getSessionSpeakers(Set<Speaker> speakers) {
        return get(speakers, Speaker::getName);
    }

}
