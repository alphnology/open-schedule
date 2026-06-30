package com.alphnology.alfiobridge.lookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceTypeTest {

    @Test
    void parsesSupportedValuesCaseInsensitively() {
        assertEquals(ReferenceType.TICKET_UUID, ReferenceType.fromValue("ticket_uuid"));
        assertEquals(ReferenceType.ORDER_CODE, ReferenceType.fromValue("ORDER_CODE"));
        assertEquals(ReferenceType.RESERVATION_ID, ReferenceType.fromValue(" reservation_id "));
    }

    @Test
    void rejectsUnsupportedValues() {
        assertThrows(IllegalArgumentException.class, () -> ReferenceType.fromValue("ticket_reference"));
    }
}
