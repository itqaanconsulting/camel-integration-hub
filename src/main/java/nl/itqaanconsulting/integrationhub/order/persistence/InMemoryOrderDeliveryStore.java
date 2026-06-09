package nl.itqaanconsulting.integrationhub.order.persistence;

import nl.itqaanconsulting.integrationhub.order.domain.OrderDelivery;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderDeliveryStore {

    private final Map<UUID, OrderDelivery> deliveries = new ConcurrentHashMap<>();

    public void save(OrderDelivery delivery) {
        deliveries.put(delivery.integrationId(), delivery);
    }

    public List<OrderDelivery> findAll() {
        return deliveries.values().stream()
                .sorted(Comparator.comparing(OrderDelivery::updatedAt).reversed())
                .toList();
    }

    public List<OrderDelivery> findDeadLetters() {
        return findAll().stream()
                .filter(delivery -> "DEAD_LETTER".equals(delivery.status()))
                .toList();
    }

    public Map<String, Long> countByStatus() {
        return deliveries.values().stream()
                .collect(Collectors.groupingBy(OrderDelivery::status, Collectors.counting()));
    }

    public void clear() {
        deliveries.clear();
    }
}
