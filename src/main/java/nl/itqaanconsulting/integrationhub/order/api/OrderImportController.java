package nl.itqaanconsulting.integrationhub.order.api;

import jakarta.validation.Valid;
import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryFileImportStore;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations/orders")
public class OrderImportController {

    private final ProducerTemplate producerTemplate;
    private final InMemoryOrderStore orderStore;
    private final InMemoryFileImportStore fileImportStore;

    public OrderImportController(
            ProducerTemplate producerTemplate,
            InMemoryOrderStore orderStore,
            InMemoryFileImportStore fileImportStore
    ) {
        this.producerTemplate = producerTemplate;
        this.orderStore = orderStore;
        this.fileImportStore = fileImportStore;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportOrderResponse importOrder(@Valid @RequestBody ImportOrderRequest request) {
        CanonicalOrder order = producerTemplate.requestBody("direct:import-order", request, CanonicalOrder.class);
        return ImportOrderResponse.from(order);
    }

    @PostMapping(path = "/csv", consumes = "text/csv")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CsvImportResponse importCsv(@RequestBody String csv) {
        return producerTemplate.requestBody("direct:import-order-csv", csv, CsvImportResponse.class);
    }

    @GetMapping
    public List<ImportOrderResponse> findAll() {
        return orderStore.findAll().stream()
                .map(ImportOrderResponse::from)
                .toList();
    }

    @GetMapping("/summary")
    public IntegrationSummaryResponse summary() {
        return new IntegrationSummaryResponse(orderStore.size(), orderStore.countByLane());
    }

    @GetMapping("/file-imports")
    public List<FileImportResponse> findFileImports() {
        return fileImportStore.findAll().stream()
                .map(FileImportResponse::from)
                .toList();
    }
}
