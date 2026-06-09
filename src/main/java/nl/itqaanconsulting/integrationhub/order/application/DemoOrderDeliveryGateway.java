package nl.itqaanconsulting.integrationhub.order.application;

import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DemoOrderDeliveryGateway {

    public static final String DELIVERY_ATTEMPTS_HEADER = "deliveryAttempts";
    private static final String UNAVAILABLE_SOURCE = "DEMO-UNAVAILABLE";

    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();

    public void deliver(Exchange exchange) {
        CanonicalOrder order = exchange.getMessage().getBody(CanonicalOrder.class);
        int attempt = attempts.merge(order.integrationId(), 1, Integer::sum);
        exchange.getMessage().setHeader(DELIVERY_ATTEMPTS_HEADER, attempt);

        if (UNAVAILABLE_SOURCE.equals(order.sourceSystem())) {
            throw new DownstreamDeliveryException("Demo downstream service is unavailable");
        }
    }
}
