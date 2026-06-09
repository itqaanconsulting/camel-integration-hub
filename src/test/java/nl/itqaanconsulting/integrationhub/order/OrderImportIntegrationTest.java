package nl.itqaanconsulting.integrationhub.order;

import com.github.tomakehurst.wiremock.WireMockServer;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderStore;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryFileImportStore;
import nl.itqaanconsulting.integrationhub.order.persistence.InMemoryOrderDeliveryStore;
import org.apache.camel.Exchange;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderImportIntegrationTest {

    private static final WireMockServer DOWNSTREAM_API = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        DOWNSTREAM_API.start();
    }

    @DynamicPropertySource
    static void deliveryProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.delivery.url", () -> DOWNSTREAM_API.baseUrl() + "/api/orders");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private InMemoryOrderStore orderStore;

    @Autowired
    private InMemoryFileImportStore fileImportStore;

    @Autowired
    private InMemoryOrderDeliveryStore deliveryStore;

    @BeforeEach
    void clearStore() {
        DOWNSTREAM_API.resetAll();
        DOWNSTREAM_API.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/orders"))
                .atPriority(10)
                .willReturn(aResponse().withStatus(202)));
        DOWNSTREAM_API.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/orders"))
                .atPriority(1)
                .withRequestBody(matchingJsonPath("$.sourceSystem", equalTo("DEMO-UNAVAILABLE")))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Downstream service is unavailable\"}")));
        orderStore.clear();
        fileImportStore.clear();
        deliveryStore.clear();
    }

    @AfterAll
    static void stopDownstreamApi() {
        DOWNSTREAM_API.stop();
    }

    @Test
    void startsCamelRoutes() {
        assertThat(camelContext.getRouteController().getRouteStatus("order-import-route").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("csv-order-import-route").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("order-file-processing-route").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("sftp-order-file-route").isStopped()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("store-canonical-order").isStarted()).isTrue();
        assertThat(camelContext.getRouteController().getRouteStatus("deliver-order-route").isStarted()).isTrue();
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

        mockMvc.perform(get("/api/integrations/orders/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DELIVERED"))
                .andExpect(jsonPath("$[0].attempts").value(1));

        DOWNSTREAM_API.verify(1, postRequestedFor(urlEqualTo("/api/orders"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.processingLane", equalTo("STANDARD"))));
    }

    @Test
    void retriesFailedDeliveryAndMovesItToDeadLetterStore() throws Exception {
        mockMvc.perform(post("/api/integrations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("demo-unavailable", "failure@example.com", "249.00", "EUR")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sourceSystem").value("DEMO-UNAVAILABLE"));

        mockMvc.perform(get("/api/integrations/orders/dead-letters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalOrderId").value("EXT-1001"))
                .andExpect(jsonPath("$[0].status").value("DEAD_LETTER"))
                .andExpect(jsonPath("$[0].attempts").value(3))
                .andExpect(jsonPath("$[0].errorMessage").value(containsString("statusCode: 500")));

        DOWNSTREAM_API.verify(3, postRequestedFor(urlEqualTo("/api/orders"))
                .withRequestBody(matchingJsonPath("$.sourceSystem", equalTo("DEMO-UNAVAILABLE"))));
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
    void exposesIntegrationOverviewAndMetrics() throws Exception {
        mockMvc.perform(post("/api/integrations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("ERP", "monitoring@example.com", "125.00", "EUR")))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/integrations/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.deliveriesByStatus.DELIVERED").value(1))
                .andExpect(jsonPath("$.routes[?(@.routeId == 'deliver-order-route')].status").value("Started"));

        mockMvc.perform(get("/actuator/metrics/integration.orders.delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("integration.orders.delivery"));
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

    @Test
    void processesPickedUpFileAndRecordsImportResult() throws Exception {
        String csv = """
                externalOrderId,sourceSystem,customerEmail,totalAmount,currency
                FILE-1001,sftp-partner,partner@example.com,450.00,EUR
                """;

        producerTemplate.requestBodyAndHeader(
                "direct:import-order-file",
                csv,
                Exchange.FILE_NAME_ONLY,
                "partner-orders.csv"
        );

        mockMvc.perform(get("/api/integrations/orders/file-imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("partner-orders.csv"))
                .andExpect(jsonPath("$[0].totalRows").value(1))
                .andExpect(jsonPath("$[0].acceptedRows").value(1))
                .andExpect(jsonPath("$[0].rejectedRows").value(0));
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
