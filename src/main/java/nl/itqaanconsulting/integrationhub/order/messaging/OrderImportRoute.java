package nl.itqaanconsulting.integrationhub.order.messaging;

import nl.itqaanconsulting.integrationhub.order.application.OrderDeliveryRecorder;
import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import nl.itqaanconsulting.integrationhub.order.persistence.OrderDeliveryStore;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderImportRoute extends RouteBuilder {

    private static final String HIGH_VALUE_LANE = "HIGH_VALUE";
    private static final String STANDARD_LANE = "STANDARD";
    static final String DUPLICATE_ORDER_PROPERTY = "duplicateOrder";

    private final InMemoryOrderStore orderStore;
    private final OrderDeliveryStore deliveryStore;

    public OrderImportRoute(InMemoryOrderStore orderStore, OrderDeliveryStore deliveryStore) {
        this.orderStore = orderStore;
        this.deliveryStore = deliveryStore;
    }

    @Override
    public void configure() {
        // HTTP error responses and transport failures (connection refused, reset, timeout)
        // are both transient delivery problems: retry them, then record a dead letter.
        onException(HttpOperationFailedException.class, IOException.class)
                .maximumRedeliveries("{{integration.delivery.maximum-redeliveries}}")
                .redeliveryDelay("{{integration.delivery.redelivery-delay}}")
                .useExponentialBackOff()
                .backOffMultiplier(2)
                .onRedelivery(exchange -> {
                    Integer attempts = exchange.getProperty(
                            OrderDeliveryRecorder.DELIVERY_ATTEMPTS_PROPERTY,
                            Integer.class
                    );
                    exchange.setProperty(
                            OrderDeliveryRecorder.DELIVERY_ATTEMPTS_PROPERTY,
                            attempts == null ? 2 : attempts + 1
                    );
                })
                .handled(true)
                .bean("orderDeliveryRecorder", "recordDeadLetter")
                .setBody(exchangeProperty("deliveryOrder"));

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

        from("direct:import-order-file")
                .routeId("order-file-processing-route")
                .convertBodyTo(String.class)
                .to("direct:import-order-csv")
                .bean("fileImportRecorder", "record");

        from("sftp://{{integration.sftp.username}}@{{integration.sftp.host}}:{{integration.sftp.port}}/{{integration.sftp.directory}}"
                + "?password={{integration.sftp.password}}"
                + "&include=.*\\.csv"
                + "&delay={{integration.sftp.poll-delay}}"
                + "&readLock=changed"
                + "&move=.processed/${file:name}"
                + "&strictHostKeyChecking={{integration.sftp.strict-host-key-checking}}"
                + "&useUserKnownHostsFile={{integration.sftp.use-user-known-hosts-file}}"
                + "&bridgeErrorHandler=true")
                .routeId("sftp-order-file-route")
                .autoStartup("{{integration.sftp.enabled}}")
                .to("direct:import-order-file");

        // Idempotent receiver: an order is identified by source system and external order id.
        // A resubmitted order returns the original result instead of being delivered twice.
        from("direct:store-order")
                .routeId("store-canonical-order")
                .process(exchange -> {
                    CanonicalOrder order = exchange.getMessage().getBody(CanonicalOrder.class);
                    deliveryStore.findBySourceSystemAndExternalOrderId(order.sourceSystem(), order.externalOrderId())
                            .ifPresentOrElse(
                                    existing -> {
                                        exchange.setProperty(DUPLICATE_ORDER_PROPERTY, true);
                                        exchange.getMessage().setBody(existing.toCanonicalOrder());
                                    },
                                    () -> orderStore.save(order)
                            );
                })
                .choice()
                    .when(exchangeProperty(DUPLICATE_ORDER_PROPERTY).isEqualTo(true))
                        .log("Duplicate order ${body.sourceSystem}:${body.externalOrderId} ignored")
                    .otherwise()
                        .to("direct:deliver-order")
                .end();

        from("direct:deliver-order")
                .routeId("deliver-order-route")
                .setProperty("deliveryOrder", body())
                .setProperty(OrderDeliveryRecorder.DELIVERY_ATTEMPTS_PROPERTY, constant(1))
                .marshal().json(JsonLibrary.Jackson)
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("{{integration.delivery.url}}"
                        + "?throwExceptionOnFailure=true"
                        + "&bridgeEndpoint=true")
                .setBody(exchangeProperty("deliveryOrder"))
                .bean("orderDeliveryRecorder", "recordDelivered");
    }
}
