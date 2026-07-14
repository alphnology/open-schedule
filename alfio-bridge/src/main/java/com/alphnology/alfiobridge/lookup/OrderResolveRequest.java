package com.alphnology.alfiobridge.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderResolveRequest(
        @NotBlank String eventSlug,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9]{8}$", message = "orderCode must contain exactly 8 alphanumeric characters.")
        String orderCode
) {
}
