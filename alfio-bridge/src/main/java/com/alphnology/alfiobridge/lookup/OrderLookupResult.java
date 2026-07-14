package com.alphnology.alfiobridge.lookup;

import java.util.List;

public record OrderLookupResult(
        boolean resolved,
        String eventSlug,
        String reservationId,
        String reservationCode,
        String reservationStatus,
        String source,
        List<OrderParticipantLookupResult> participants
) {
}
