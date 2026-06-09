package nl.itqaanconsulting.integrationhub.order.domain;

import java.time.Instant;
import java.util.UUID;

public record FileImportResult(
        UUID importId,
        String fileName,
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        Instant processedAt
) {
}
