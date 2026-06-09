package nl.itqaanconsulting.integrationhub.order.domain;

import java.time.Instant;
import java.util.UUID;

public record OrderDelivery(
        UUID integrationId,
        String externalOrderId,
        String status,
        int attempts,
        String errorMessage,
        Instant updatedAt
) {
}
