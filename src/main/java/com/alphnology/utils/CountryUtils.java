package com.alphnology.utils;

import com.alphnology.data.Country;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author me@fredpena.dev
 * @created 14/06/2025  - 11:14
 */
public class CountryUtils {

    private CountryUtils() {

    }

    public static List<Country> getCountryNamesWithCodes() {
        return Arrays.stream(Locale.getISOCountries())
                .map(code -> {
                    Locale locale = Locale.of("", code);
                    return new Country(locale.getDisplayCountry(Locale.ENGLISH), code);
                })
                .sorted()
                .toList();
    }

    public static List<String> getAvailableZoneIds() {
        return ZoneId.getAvailableZoneIds().stream()
                .sorted()
                .map(id -> {
                    ZoneId zoneId = ZoneId.of(id);
                    ZonedDateTime now = ZonedDateTime.now(zoneId);
                    ZoneOffset offset = now.getOffset();
                    return String.format("%s (UTC%s)", id, offset);
                })
                .toList();

    }
}
