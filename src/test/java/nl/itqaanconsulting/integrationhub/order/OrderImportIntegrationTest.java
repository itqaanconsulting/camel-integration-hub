package nl.itqaanconsulting.integrationhub.order;

import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private InMemoryOrderStore orderStore;

    @BeforeEach
    void clearStore() {
        orderStore.clear();
    }

    @Test
    void startsCamelRoutes() {
        assertThat(camelContext.getRouteController().getRouteStatus("order-import-route").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("csv-order-import-route").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("store-canonical-order").isStarted()).isTrue();
    }

    @Test
    void normalizesAndRoutesStandardOrder() throws Exception {
        mockMvc.perform(post("/api/integrations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(" web-shop ", "customer@EXAMPLE.com", "149.95", "eur")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceSystem").value("WEB-SHOP"))
                .andExpect(jsonPath("$.customerEmail").value("customer@example.com"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.processingLane").value("STANDARD"));
    }

    @Test
    void routesHighValueOrderToDedicatedLane() throws Exception {
        mockMvc.perform(post("/api/integrations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("ERP", "business@example.com", "2500.00", "EUR")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.processingLane").value("HIGH_VALUE"));

        mockMvc.perform(get("/api/integrations/orders/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.ordersByLane.HIGH_VALUE").value(1));
    }

    @Test
    void rejectsInvalidOrderBeforeEnteringRoute() throws Exception {
        mockMvc.perform(post("/api/integrations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("ERP", "not-an-email", "0", "EU")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importsValidCsvRowsAndReportsRejectedRows() throws Exception {
        String csv = """
                externalOrderId,sourceSystem,customerEmail,totalAmount,currency
                CSV-1001,web-shop,first@example.com,125.50,eur
                CSV-1002,erp,invalid-email,250.00,EUR
                CSV-1003,marketplace,third@example.com,1750.00,USD
                """;

        mockMvc.perform(post("/api/integrations/orders/csv")
                        .contentType("text/csv")
                        .content(csv))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.acceptedRows").value(2))
                .andExpect(jsonPath("$.rejectedRows").value(1))
                .andExpect(jsonPath("$.importedOrders[0].processingLane").value("STANDARD"))
                .andExpect(jsonPath("$.importedOrders[1].processingLane").value("HIGH_VALUE"))
                .andExpect(jsonPath("$.rejectedOrders[0].rowNumber").value(3))
                .andExpect(jsonPath("$.rejectedOrders[0].externalOrderId").value("CSV-1002"));

        mockMvc.perform(get("/api/integrations/orders/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.ordersByLane.STANDARD").value(1))
                .andExpect(jsonPath("$.ordersByLane.HIGH_VALUE").value(1));
    }

    private String orderJson(String sourceSystem, String email, String amount, String currency) {
        return """
                {
                  "externalOrderId": "EXT-1001",
                  "sourceSystem": "%s",
                  "customerEmail": "%s",
                  "totalAmount": %s,
                  "currency": "%s"
                }
                """.formatted(sourceSystem, email, amount, currency);
    }
}
