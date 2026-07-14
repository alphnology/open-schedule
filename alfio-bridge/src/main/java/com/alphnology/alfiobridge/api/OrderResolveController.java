package com.alphnology.alfiobridge.api;

import com.alphnology.alfiobridge.lookup.AlfioJdbcReservationLookupService;
import com.alphnology.alfiobridge.lookup.OrderLookupResult;
import com.alphnology.alfiobridge.lookup.OrderResolveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderResolveController {

    private final AlfioJdbcReservationLookupService reservationLookupService;

    public OrderResolveController(AlfioJdbcReservationLookupService reservationLookupService) {
        this.reservationLookupService = reservationLookupService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<OrderLookupResult> resolve(@Valid @RequestBody OrderResolveRequest request) {
        return ResponseEntity.ok(reservationLookupService.resolveOrder(request));
    }
}
