package nl.itqaanconsulting.integrationhub.order.application;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import nl.itqaanconsulting.integrationhub.order.api.CsvImportResponse;
import nl.itqaanconsulting.integrationhub.order.api.ImportOrderRequest;
import nl.itqaanconsulting.integrationhub.order.api.ImportOrderResponse;
import nl.itqaanconsulting.integrationhub.order.api.RejectedOrderResponse;
import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CsvOrderBatchProcessor {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "externalOrderId",
            "sourceSystem",
            "customerEmail",
            "totalAmount",
            "currency"
    );

    private final Validator validator;
    private final ProducerTemplate producerTemplate;

    public CsvOrderBatchProcessor(Validator validator, ProducerTemplate producerTemplate) {
        this.validator = validator;
        this.producerTemplate = producerTemplate;
    }

    public CsvImportResponse process(List<Map<String, String>> rows) {
        List<ImportOrderResponse> imported = new ArrayList<>();
        List<RejectedOrderResponse> rejected = new ArrayList<>();

        for (int index = 0; index < rows.size(); index++) {
            Map<String, String> row = rows.get(index);
            int rowNumber = index + 2;
            String externalOrderId = row.getOrDefault("externalOrderId", "");

            try {
                validateHeaders(row);
                ImportOrderRequest request = toRequest(row);
                validate(request);
                CanonicalOrder order = producerTemplate.requestBody("direct:import-order", request, CanonicalOrder.class);
                imported.add(ImportOrderResponse.from(order));
            } catch (RuntimeException exception) {
                rejected.add(new RejectedOrderResponse(rowNumber, externalOrderId, exception.getMessage()));
            }
        }

        return new CsvImportResponse(rows.size(), imported.size(), rejected.size(), imported, rejected);
    }

    private ImportOrderRequest toRequest(Map<String, String> row) {
        return new ImportOrderRequest(
                row.get("externalOrderId"),
                row.get("sourceSystem"),
                row.get("customerEmail"),
                new BigDecimal(row.get("totalAmount")),
                row.get("currency")
        );
    }

    private void validate(ImportOrderRequest request) {
        Set<ConstraintViolation<ImportOrderRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(message);
    }

    private void validateHeaders(Map<String, String> row) {
        if (!row.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new IllegalArgumentException("CSV header must contain: " + String.join(", ", REQUIRED_HEADERS));
        }
    }
}
