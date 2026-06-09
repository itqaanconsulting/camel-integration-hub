package nl.itqaanconsulting.integrationhub.order.application;

import nl.itqaanconsulting.integrationhub.order.api.CsvImportResponse;
import nl.itqaanconsulting.integrationhub.order.domain.FileImportResult;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryFileImportStore;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class FileImportRecorder {

    private final InMemoryFileImportStore importStore;

    public FileImportRecorder(InMemoryFileImportStore importStore) {
        this.importStore = importStore;
    }

    public CsvImportResponse record(Exchange exchange) {
        CsvImportResponse response = exchange.getMessage().getBody(CsvImportResponse.class);
        String fileName = exchange.getMessage().getHeader(Exchange.FILE_NAME_ONLY, String.class);

        importStore.save(new FileImportResult(
                UUID.randomUUID(),
                fileName,
                response.totalRows(),
                response.acceptedRows(),
                response.rejectedRows(),
                Instant.now()
        ));
        return response;
    }
}
