package nl.itqaanconsulting.integrationhub.order.persistence;

import nl.itqaanconsulting.integrationhub.order.domain.OrderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OrderDeliveryRepository extends JpaRepository<OrderDelivery, UUID> {

    List<OrderDelivery> findAllByOrderByUpdatedAtDesc();

    List<OrderDelivery> findAllByStatusOrderByUpdatedAtDesc(String status);
}
