package com.alphnology.alfiobridge.lookup;

public record OrderParticipantLookupResult(
        String ticketId,
        String ticketPublicId,
        String ticketStatus,
        String attendeeName,
        String attendeeEmail
) {
}
