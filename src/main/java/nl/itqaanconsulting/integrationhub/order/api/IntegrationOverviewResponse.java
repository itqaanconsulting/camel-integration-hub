package nl.itqaanconsulting.integrationhub.order.api;

import java.util.List;
import java.util.Map;

public record IntegrationOverviewResponse(
        int totalOrders,
        Map<String, Long> ordersByLane,
        Map<String, Long> deliveriesByStatus,
        int fileImports,
        List<RouteStatusResponse> routes
) {
}
