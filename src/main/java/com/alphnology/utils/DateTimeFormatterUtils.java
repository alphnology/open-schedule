package com.alphnology.utils;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @author me@fredpena.dev
 * @created 25/06/2025  - 18:18
 */
public class DateTimeFormatterUtils {

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("EEEE MMM d HH:mm");
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE MMM d", Locale.of("us", "US"));
    public static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeFormatterUtils(){

    }
}
