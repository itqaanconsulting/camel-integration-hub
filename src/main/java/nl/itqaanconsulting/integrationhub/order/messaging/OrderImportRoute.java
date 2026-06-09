package nl.itqaanconsulting.integrationhub.order.messaging;

import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.springframework.stereotype.Component;

@Component
public class OrderImportRoute extends RouteBuilder {

    private static final String HIGH_VALUE_LANE = "HIGH_VALUE";
    private static final String STANDARD_LANE = "STANDARD";

    private final InMemoryOrderStore orderStore;

    public OrderImportRoute(InMemoryOrderStore orderStore) {
        this.orderStore = orderStore;
    }

    @Override
    public void configure() {
        CsvDataFormat csvDataFormat = new CsvDataFormat();
        csvDataFormat.setUseMaps(true);
        csvDataFormat.setSkipHeaderRecord(true);

        from("direct:import-order")
                .routeId("order-import-route")
                .bean("orderNormalizer", "normalize")
                .choice()
                    .when(simple("${body.totalAmount} >= 1000"))
                        .transform().body(CanonicalOrder.class, order -> order.withProcessingLane(HIGH_VALUE_LANE))
                        .to("direct:store-order")
                    .otherwise()
                        .transform().body(CanonicalOrder.class, order -> order.withProcessingLane(STANDARD_LANE))
                        .to("direct:store-order")
                .end();

        from("direct:import-order-csv")
                .routeId("csv-order-import-route")
                .unmarshal(csvDataFormat)
                .bean("csvOrderBatchProcessor", "process");

        from("direct:store-order")
                .routeId("store-canonical-order")
                .process(exchange -> orderStore.save(exchange.getMessage().getBody(CanonicalOrder.class)));
    }
}
