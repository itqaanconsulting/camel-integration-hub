package nl.itqaanconsulting.integrationhub.order.api;

import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryFileImportStore;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderDeliveryStore;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import org.apache.camel.CamelContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationOverviewController {

    private final CamelContext camelContext;
    private final InMemoryOrderStore orderStore;
    private final InMemoryOrderDeliveryStore deliveryStore;
    private final InMemoryFileImportStore fileImportStore;

    public IntegrationOverviewController(
            CamelContext camelContext,
            InMemoryOrderStore orderStore,
            InMemoryOrderDeliveryStore deliveryStore,
            InMemoryFileImportStore fileImportStore
    ) {
        this.camelContext = camelContext;
        this.orderStore = orderStore;
        this.deliveryStore = deliveryStore;
        this.fileImportStore = fileImportStore;
    }

    @GetMapping("/overview")
    public IntegrationOverviewResponse overview() {
        var routes = camelContext.getRoutes().stream()
                .map(route -> new RouteStatusResponse(
                        route.getRouteId(),
                        camelContext.getRouteController().getRouteStatus(route.getRouteId()).name()
                ))
                .toList();

        return new IntegrationOverviewResponse(
                orderStore.size(),
                orderStore.countByLane(),
                deliveryStore.countByStatus(),
                fileImportStore.size(),
                routes
        );
    }
}
