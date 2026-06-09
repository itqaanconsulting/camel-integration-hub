package nl.itqaanconsulting.integrationhub.order.api;

import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ImportOrderResponse(
        UUID integrationId,
        String externalOrderId,
        String sourceSystem,
        String customerEmail,
        BigDecimal totalAmount,
        String currency,
        String processingLane,
        Instant receivedAt
) {
    public static ImportOrderResponse from(CanonicalOrder order) {
        return new ImportOrderResponse(
                order.integrationId(),
                order.externalOrderId(),
                order.sourceSystem(),
                order.customerEmail(),
                order.totalAmount(),
                order.currency(),
                order.processingLane(),
                order.receivedAt()
        );
    }
}
