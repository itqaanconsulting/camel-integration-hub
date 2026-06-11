package nl.itqaanconsulting.integrationhub.order.persistence;

import nl.itqaanconsulting.integrationhub.order.domain.OrderDelivery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class OrderDeliveryStore {

    private final OrderDeliveryRepository repository;

    public OrderDeliveryStore(OrderDeliveryRepository repository) {
        this.repository = repository;
    }

    public void save(OrderDelivery delivery) {
        repository.save(delivery);
    }

    public List<OrderDelivery> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    public List<OrderDelivery> findDeadLetters() {
        return repository.findAllByStatusOrderByUpdatedAtDesc("DEAD_LETTER");
    }

    public Optional<OrderDelivery> findById(UUID integrationId) {
        return repository.findById(integrationId);
    }

    public Map<String, Long> countByStatus() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(OrderDelivery::getStatus, Collectors.counting()));
    }

    public void clear() {
        repository.deleteAll();
    }
}
