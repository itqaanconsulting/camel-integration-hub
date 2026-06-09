package nl.itqaanconsulting.integrationhub.order.api;

public record RejectedOrderResponse(
        int rowNumber,
        String externalOrderId,
        String reason
) {
}
