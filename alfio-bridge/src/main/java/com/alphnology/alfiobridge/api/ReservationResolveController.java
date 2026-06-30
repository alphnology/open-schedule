package com.alphnology.alfiobridge.api;

import com.alphnology.alfiobridge.lookup.ReservationLookupResult;
import com.alphnology.alfiobridge.lookup.ReservationLookupService;
import com.alphnology.alfiobridge.lookup.ReservationResolveRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationResolveController {

    private final ReservationLookupService reservationLookupService;

    public ReservationResolveController(ReservationLookupService reservationLookupService) {
        this.reservationLookupService = reservationLookupService;
    }

    @PostMapping("/resolve")
    public ResponseEntity<ReservationLookupResult> resolve(@Valid @RequestBody ReservationResolveRequest request) {
        return ResponseEntity.ok(reservationLookupService.resolve(request));
    }
}
