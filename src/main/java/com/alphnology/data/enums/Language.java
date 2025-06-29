package com.alphnology.data.enums;

import lombok.Getter;

/**
 * Represents the supported languages within the application.
 * Each language constant has a display name.
 *
 * @author me@fredpena.dev
 * @created 13/06/2025  - 23:45
 */

@Getter
public enum Language {

    /**
     * Spanish language (Dominican Republic).
     * Recommended to use the two-digit country code for the enum declaration,
     * so it can be automatically mapped with https://flagcdn.com/w80/xx.png
     * Example: https://flagcdn.com/w80/do.png
     */

    /**
     * English language (United States).
     */
    US("English"),

    /**
     * TSpanish language (Dominican Republic).
     */
    DO("Español");

    /**
     * The display name of the language.
     */
    private final String display;

    /**
     * Constructs a new Language enum constant with the specified display name.
     *
     * @param display The human-readable name of the language.
     */
    Language(String display) {
        this.display = display;
    }
}
