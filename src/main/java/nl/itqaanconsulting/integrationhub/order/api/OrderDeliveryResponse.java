package nl.itqaanconsulting.integrationhub.order.api;

import nl.itqaanconsulting.integrationhub.order.domain.OrderDelivery;

import java.time.Instant;
import java.util.UUID;

public record OrderDeliveryResponse(
        UUID integrationId,
        String externalOrderId,
        String status,
        int attempts,
        String errorMessage,
        Instant updatedAt
) {
    public static OrderDeliveryResponse from(OrderDelivery delivery) {
        return new OrderDeliveryResponse(
                delivery.integrationId(),
                delivery.externalOrderId(),
                delivery.status(),
                delivery.attempts(),
                delivery.errorMessage(),
                delivery.updatedAt()
        );
    }
}
