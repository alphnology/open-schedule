package com.alphnology.alfiobridge.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationResolveRequest(
        @NotBlank String eventSlug,
        @NotNull ReferenceType referenceType,
        @NotBlank String referenceValue
) {
}
