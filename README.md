# Camel Integration Hub

Integration showcase built with Java 21, Spring Boot and Apache Camel.

The hub accepts individual JSON orders and CSV batches, and can poll CSV files from SFTP. Every input is normalized to one canonical model and uses a Camel content-based router to assign a standard or high-value processing lane.

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
GET /api/integrations/orders/deliveries
```

After an order is stored, Camel sends it to a demo downstream adapter. Successful deliveries are recorded with status `DELIVERED`.

## Retry And Dead Letters

The delivery route uses exponential backoff. A failed downstream call is retried twice, resulting in three delivery attempts. When all attempts fail, the order remains accepted and is recorded with status `DEAD_LETTER`.

Use source system `demo-unavailable` to demonstrate this scenario:

```http
POST /api/integrations/orders
Content-Type: application/json

{
  "externalOrderId": "FAIL-1001",
  "sourceSystem": "demo-unavailable",
  "customerEmail": "failure@example.com",
  "totalAmount": 249.00,
  "currency": "EUR"
}
```

Inspect the delivery result:

```http
GET /api/integrations/orders/dead-letters
```

The response shows status `DEAD_LETTER`, three attempts and the final error message. Retry settings can be overridden with `DELIVERY_MAXIMUM_REDELIVERIES` and `DELIVERY_REDELIVERY_DELAY`.

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

## SFTP Pickup

The SFTP consumer is disabled by default so the application can run without external infrastructure. Configure the connection through `integration.sftp.*` and enable it with:

```powershell
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=sftp"
```

The route:

1. Polls `*.csv` files from the configured upload directory.
2. Uses a changed-file read lock to avoid partially written files.
3. Sends the content through the existing CSV and canonical order routes.
4. Moves processed files into `.processed`.
5. Records the filename and accepted/rejected totals.

Inspect processed file imports:

```http
GET /api/integrations/orders/file-imports
```

Upload the included demo batch:

```powershell
docker cp demo/sftp-orders.csv camel-integration-sftp:/home/camel/upload/orders.csv
```

Within a few seconds Camel picks up the file. The demo batch contains two valid orders and one rejected row. The source file is moved to the `.processed` directory on the SFTP server.

Connection settings can be overridden with `SFTP_HOST`, `SFTP_PORT`, `SFTP_USERNAME`, `SFTP_PASSWORD`, `SFTP_DIRECTORY` and `SFTP_POLL_DELAY`.

The local demo disables strict host-key checking because the disposable container generates its own key. For a real environment, set `SFTP_STRICT_HOST_KEY_CHECKING=yes` and `SFTP_USE_USER_KNOWN_HOSTS_FILE=true`, and provision the server key in the runtime user's `known_hosts` file.

## Test

```powershell
mvn test
```

## Planned Showcase Flows

- JSON and XML transformation
- External API delivery with WireMock
- Route metrics and browser demo
