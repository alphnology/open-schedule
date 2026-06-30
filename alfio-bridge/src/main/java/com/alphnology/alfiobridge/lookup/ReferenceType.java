package com.alphnology.alfiobridge.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ReferenceType {
    TICKET_UUID("ticket_uuid"),
    ORDER_CODE("order_code"),
    RESERVATION_ID("reservation_id");

    private final String value;

    ReferenceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ReferenceType fromValue(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ReferenceType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported referenceType: " + value);
    }
}
