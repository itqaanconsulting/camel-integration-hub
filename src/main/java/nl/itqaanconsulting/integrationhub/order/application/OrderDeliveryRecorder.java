package nl.itqaanconsulting.integrationhub.order.application;

import io.micrometer.core.instrument.MeterRegistry;
import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import nl.itqaanconsulting.integrationhub.order.domain.OrderDelivery;
import nl.itqaanconsulting.integrationhub.order.persistence.OrderDeliveryStore;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
public class OrderDeliveryRecorder {

    public static final String DELIVERY_ATTEMPTS_PROPERTY = "deliveryAttempts";

    private final OrderDeliveryStore deliveryStore;
    private final MeterRegistry meterRegistry;

    public OrderDeliveryRecorder(OrderDeliveryStore deliveryStore, MeterRegistry meterRegistry) {
        this.deliveryStore = deliveryStore;
        this.meterRegistry = meterRegistry;
    }

    public void recordDelivered(Exchange exchange) {
        save(exchange, "DELIVERED", null);
    }

    public void recordDeadLetter(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        save(exchange, "DEAD_LETTER", exception == null ? "Delivery failed" : exception.getMessage());
    }

    private void save(Exchange exchange, String status, String errorMessage) {
        CanonicalOrder order = exchange.getProperty("deliveryOrder", CanonicalOrder.class);
        Integer attempts = exchange.getProperty(DELIVERY_ATTEMPTS_PROPERTY, Integer.class);

        deliveryStore.save(new OrderDelivery(
                order,
                status,
                attempts == null ? 1 : attempts,
                errorMessage
        ));
        meterRegistry.counter("integration.orders.delivery", "status", status).increment();
    }
}
