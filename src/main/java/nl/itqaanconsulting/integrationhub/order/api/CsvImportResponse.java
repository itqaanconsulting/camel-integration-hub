package nl.itqaanconsulting.integrationhub.order.api;

import java.util.List;

public record CsvImportResponse(
        int totalRows,
        int acceptedRows,
        int rejectedRows,
        List<ImportOrderResponse> importedOrders,
        List<RejectedOrderResponse> rejectedOrders
) {
}
