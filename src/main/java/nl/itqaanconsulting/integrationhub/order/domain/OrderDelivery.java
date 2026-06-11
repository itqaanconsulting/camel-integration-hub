package nl.itqaanconsulting.integrationhub.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_deliveries")
public class OrderDelivery {

    @Id
    @Column(name = "integration_id")
    private UUID integrationId;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "processing_lane", nullable = false, length = 30)
    private String processingLane;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderDelivery() {
    }

    public OrderDelivery(CanonicalOrder order, String status, int attempts, String errorMessage) {
        this.integrationId = order.integrationId();
        this.externalOrderId = order.externalOrderId();
        this.sourceSystem = order.sourceSystem();
        this.customerEmail = order.customerEmail();
        this.totalAmount = order.totalAmount();
        this.currency = order.currency();
        this.processingLane = order.processingLane();
        this.receivedAt = order.receivedAt();
        this.status = status;
        this.attempts = attempts;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public CanonicalOrder toCanonicalOrder() {
        return new CanonicalOrder(
                integrationId,
                externalOrderId,
                sourceSystem,
                customerEmail,
                totalAmount,
                currency,
                processingLane,
                receivedAt
        );
    }

    public UUID getIntegrationId() {
        return integrationId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
