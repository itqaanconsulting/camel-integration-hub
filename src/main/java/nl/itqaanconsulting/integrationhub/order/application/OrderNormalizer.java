package nl.itqaanconsulting.integrationhub.order.application;

import nl.itqaanconsulting.integrationhub.order.api.ImportOrderRequest;
import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class OrderNormalizer {

    public CanonicalOrder normalize(ImportOrderRequest request) {
        return new CanonicalOrder(
                UUID.randomUUID(),
                request.externalOrderId().trim(),
                request.sourceSystem().trim().toUpperCase(Locale.ROOT),
                request.customerEmail().trim().toLowerCase(Locale.ROOT),
                request.totalAmount(),
                request.currency().trim().toUpperCase(Locale.ROOT),
                null,
                Instant.now()
        );
    }
}
