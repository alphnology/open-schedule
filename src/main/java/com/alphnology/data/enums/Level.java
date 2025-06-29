package com.alphnology.data.enums;

import lombok.Getter;

/**
 * @author me@fredpena.dev
 * @created 13/06/2025  - 23:45
 */

@Getter
public enum Level {

    B("Beginner"), I("Intermediate"), D("Advanced"), A("All");

    private final String display;

    Level(String display) {
        this.display = display;
    }
}
