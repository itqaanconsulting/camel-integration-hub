package nl.itqaanconsulting.integrationhub.order.api;

import nl.itqaanconsulting.integrationhub.order.domain.FileImportResult;

import java.time.Instant;
import java.util.UUID;

public record FileImportResponse(
        UUID importId,
        String fileName,
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        Instant processedAt
) {
    public static FileImportResponse from(FileImportResult result) {
        return new FileImportResponse(
                result.importId(),
                result.fileName(),
                result.totalRows(),
                result.acceptedRows(),
                result.rejectedRows(),
                result.processedAt()
        );
    }
}
