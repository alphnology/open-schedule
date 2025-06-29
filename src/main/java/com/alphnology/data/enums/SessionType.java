package com.alphnology.data.enums;

import lombok.Getter;

/**
 * @author me@fredpena.dev
 * @created 13/06/2025  - 23:45
 */

@Getter
public enum SessionType {

    T("Talk"), W("Workshop"), CB("Coffee Break"), L("Lunch"), K("Keynote"), P("Panel"), OS("Open Space");

    private final String display;

    SessionType(String display) {
        this.display = display;
    }
}
