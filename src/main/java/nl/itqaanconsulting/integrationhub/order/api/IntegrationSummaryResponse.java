package nl.itqaanconsulting.integrationhub.order.api;

import java.util.Map;

public record IntegrationSummaryResponse(
        int totalOrders,
        Map<String, Long> ordersByLane
) {
}
