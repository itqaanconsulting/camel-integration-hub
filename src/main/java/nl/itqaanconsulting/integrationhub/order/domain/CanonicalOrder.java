package nl.itqaanconsulting.integrationhub.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CanonicalOrder(
        UUID integrationId,
        String externalOrderId,
        String sourceSystem,
        String customerEmail,
        BigDecimal totalAmount,
        String currency,
        String processingLane,
        Instant receivedAt
) {
    public CanonicalOrder withProcessingLane(String lane) {
        return new CanonicalOrder(
                integrationId,
                externalOrderId,
                sourceSystem,
                customerEmail,
                totalAmount,
                currency,
                lane,
                receivedAt
        );
    }
}
