package nl.itqaanconsulting.integrationhub.order.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ImportOrderRequest(
        @NotBlank @Size(max = 80) String externalOrderId,
        @NotBlank @Size(max = 30) String sourceSystem,
        @NotBlank @Email @Size(max = 160) String customerEmail,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
