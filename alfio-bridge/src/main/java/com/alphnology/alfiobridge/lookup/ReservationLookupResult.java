package com.alphnology.alfiobridge.lookup;

public record ReservationLookupResult(
        boolean resolved,
        String eventSlug,
        String reservationId,
        String reservationCode,
        String ticketId,
        String ticketPublicId,
        String reservationStatus,
        String ticketStatus,
        String attendeeName,
        String attendeeEmail,
        String source
) {
}
