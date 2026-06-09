# Camel Integration Hub

Integration showcase built with Java 21, Spring Boot and Apache Camel.

The hub accepts individual JSON orders and CSV batches, normalizes them to one canonical model and uses a Camel content-based router to assign a standard or high-value processing lane.

## Current Flow

```text
REST request
    -> validation
    -> Camel direct endpoint
    -> canonical model mapping
    -> content-based routing
    -> in-memory order store
```

CSV batches use Camel CSV unmarshalling. Each valid row enters the same canonical order route as a JSON request, while invalid rows are returned in a reject report.

## Run

```powershell
mvn spring-boot:run
```

The application runs on `http://localhost:8083`.

Import an order:

```http
POST /api/integrations/orders
Content-Type: application/json

{
  "externalOrderId": "EXT-1001",
  "sourceSystem": "web-shop",
  "customerEmail": "customer@example.com",
  "totalAmount": 149.95,
  "currency": "eur"
}
```

Inspect results:

```http
GET /api/integrations/orders
GET /api/integrations/orders/summary
```

Import a CSV batch:

```http
POST /api/integrations/orders/csv
Content-Type: text/csv

externalOrderId,sourceSystem,customerEmail,totalAmount,currency
CSV-1001,web-shop,first@example.com,125.50,EUR
CSV-1002,erp,invalid-email,250.00,EUR
CSV-1003,marketplace,third@example.com,1750.00,USD
```

The response contains accepted and rejected counts, imported canonical orders and a reason for every rejected row.

## Test

```powershell
mvn test
```

## Planned Showcase Flows

- SFTP file pickup
- JSON and XML transformation
- External API delivery with WireMock
- Dead-letter handling and redelivery
- Route metrics and browser demo
