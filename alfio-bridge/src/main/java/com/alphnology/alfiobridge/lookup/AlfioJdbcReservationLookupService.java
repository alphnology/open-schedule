package com.alphnology.alfiobridge.lookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AlfioJdbcReservationLookupService implements ReservationLookupService {

    private static final String BASE_SELECT = """
            select
                e.short_name as event_slug,
                btrim(tr.id) as reservation_id,
                upper(substring(btrim(tr.id), 1, 8)) as reservation_code,
                btrim(t.uuid) as ticket_id,
                t.public_uuid::text as ticket_public_id,
                tr.status as reservation_status,
                t.status as ticket_status,
                coalesce(
                    nullif(trim(t.full_name), ''),
                    nullif(trim(tr.full_name), ''),
                    nullif(trim(concat_ws(' ', t.first_name, t.last_name)), ''),
                    nullif(trim(concat_ws(' ', tr.first_name, tr.last_name)), '')
                ) as attendee_name,
                coalesce(
                    nullif(trim(t.email_address), ''),
                    nullif(trim(tr.email_address), '')
                ) as attendee_email
            from ticket t
            join tickets_reservation tr on tr.id = t.tickets_reservation_id
            join event e on e.id = tr.event_id_fk
            where e.short_name = :eventSlug
            """;

    private static final Logger log = LoggerFactory.getLogger(AlfioJdbcReservationLookupService.class);

    private final JdbcClient jdbcClient;

    public AlfioJdbcReservationLookupService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ReservationLookupResult resolve(ReservationResolveRequest request) {
        String eventSlug = normalizeText(request.eventSlug());
        String referenceValue = normalizeText(request.referenceValue());
        String sql = switch (request.referenceType()) {
            case TICKET_UUID -> BASE_SELECT + " and btrim(t.uuid) = :referenceValue limit 1";
            case ORDER_CODE -> BASE_SELECT + " and upper(substring(btrim(tr.id), 1, 8)) = :referenceValue limit 1";
            case RESERVATION_ID -> BASE_SELECT + " and btrim(tr.id) = :referenceValue limit 1";
        };

        validateReferenceValue(request.referenceType(), referenceValue);

        log.info("Resolving alf.io reference. eventSlug='{}', referenceType={}, referenceValue='{}'",
                eventSlug, request.referenceType(), maskReference(request.referenceType(), referenceValue));

        Map<String, Object> row = jdbcClient.sql(sql)
                .param("eventSlug", eventSlug)
                .param("referenceValue", normalizeQueryValue(request.referenceType(), referenceValue))
                .query(this::mapRow)
                .optional()
                .orElse(null);

        if (row == null || row.isEmpty()) {
            throw new ReferenceNotFoundException("No alf.io reservation matches the supplied reference.");
        }

        return new ReservationLookupResult(
                true,
                asString(row.get("event_slug")),
                asString(row.get("reservation_id")),
                asString(row.get("reservation_code")),
                asString(row.get("ticket_id")),
                asString(row.get("ticket_public_id")),
                asString(row.get("reservation_status")),
                asString(row.get("ticket_status")),
                asString(row.get("attendee_name")),
                asString(row.get("attendee_email")),
                "database"
        );
    }

    private static String normalizeQueryValue(ReferenceType referenceType, String referenceValue) {
        return referenceType == ReferenceType.ORDER_CODE
                ? referenceValue.toUpperCase()
                : referenceValue;
    }

    private static void validateReferenceValue(ReferenceType referenceType, String referenceValue) {
        if (referenceType == ReferenceType.ORDER_CODE) {
            if (referenceValue.length() != 8) {
                throw new IllegalArgumentException("Order codes must contain exactly 8 characters.");
            }
            return;
        }
        try {
            UUID.fromString(referenceValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(referenceType.getValue() + " must be a valid UUID.");
        }
    }

    private static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.trim();
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("event_slug", rs.getString("event_slug"));
        row.put("reservation_id", rs.getString("reservation_id"));
        row.put("reservation_code", rs.getString("reservation_code"));
        row.put("ticket_id", rs.getString("ticket_id"));
        row.put("ticket_public_id", rs.getString("ticket_public_id"));
        row.put("reservation_status", rs.getString("reservation_status"));
        row.put("ticket_status", rs.getString("ticket_status"));
        row.put("attendee_name", rs.getString("attendee_name"));
        row.put("attendee_email", rs.getString("attendee_email"));
        return row;
    }

    private static String maskReference(ReferenceType referenceType, String referenceValue) {
        if (referenceType == ReferenceType.ORDER_CODE) {
            return referenceValue;
        }
        return referenceValue.length() <= 8
                ? referenceValue
                : referenceValue.substring(0, 8) + "...";
    }
}
