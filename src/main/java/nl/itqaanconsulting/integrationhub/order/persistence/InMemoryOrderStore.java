package nl.itqaanconsulting.integrationhub.order.persistence;

import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryOrderStore {

    private final Map<String, CanonicalOrder> orders = new ConcurrentHashMap<>();

    public void save(CanonicalOrder order) {
        orders.put(order.sourceSystem() + ":" + order.externalOrderId(), order);
    }

    public List<CanonicalOrder> findAll() {
        return orders.values().stream()
                .sorted(Comparator.comparing(CanonicalOrder::receivedAt).reversed())
                .toList();
    }

    public int size() {
        return orders.size();
    }

    public Map<String, Long> countByLane() {
        return orders.values().stream()
                .collect(Collectors.groupingBy(CanonicalOrder::processingLane, Collectors.counting()));
    }

    public void clear() {
        orders.clear();
    }
}
