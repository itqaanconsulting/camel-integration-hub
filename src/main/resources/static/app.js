const elements = {
    health: document.querySelector("#health-badge"),
    totalOrders: document.querySelector("#total-orders"),
    deliveredOrders: document.querySelector("#delivered-orders"),
    deadLetterOrders: document.querySelector("#dead-letter-orders"),
    fileImports: document.querySelector("#file-imports"),
    routeCount: document.querySelector("#route-count"),
    routeList: document.querySelector("#route-list"),
    deliveryRows: document.querySelector("#delivery-rows"),
    form: document.querySelector("#order-form"),
    message: document.querySelector("#form-message"),
    submit: document.querySelector("#submit-button"),
    failure: document.querySelector("#failure-button"),
    refresh: document.querySelector("#refresh-button")
};

function createOrderId(prefix = "DEMO") {
    return `${prefix}-${Date.now().toString().slice(-7)}`;
}

function setInitialOrderId() {
    document.querySelector("#external-order-id").value = createOrderId();
}

function badge(status) {
    const cssClass = status === "Started" || status === "DELIVERED" ? "success"
        : status === "DEAD_LETTER" ? "failure" : "neutral";
    return `<span class="badge ${cssClass}">${status.replace("_", " ")}</span>`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const body = await response.text();
        throw new Error(body || `Request failed with status ${response.status}`);
    }
    return response.json();
}

async function refreshDashboard() {
    const [health, overview, deliveries] = await Promise.all([
        request("/actuator/health"),
        request("/api/integrations/overview"),
        request("/api/integrations/orders/deliveries")
    ]);

    elements.health.textContent = health.status === "UP" ? "System healthy" : health.status;
    elements.health.className = `badge ${health.status === "UP" ? "success" : "failure"}`;
    elements.totalOrders.textContent = overview.totalOrders;
    elements.deliveredOrders.textContent = overview.deliveriesByStatus.DELIVERED ?? 0;
    elements.deadLetterOrders.textContent = overview.deliveriesByStatus.DEAD_LETTER ?? 0;
    elements.fileImports.textContent = overview.fileImports;
    elements.routeCount.textContent = `${overview.routes.length} routes`;
    elements.routeList.innerHTML = overview.routes
        .map(route => `<div class="route"><code>${escapeHtml(route.routeId)}</code>${badge(route.status)}</div>`)
        .join("");

    elements.deliveryRows.innerHTML = deliveries.length === 0
        ? '<tr><td colspan="5" class="empty">No deliveries yet</td></tr>'
        : deliveries.map(delivery => `
            <tr>
                <td><code>${escapeHtml(delivery.externalOrderId)}</code></td>
                <td>${badge(delivery.status)}</td>
                <td>${delivery.attempts}</td>
                <td>${new Date(delivery.updatedAt).toLocaleTimeString()}</td>
                <td class="result">${escapeHtml(delivery.errorMessage || "Accepted by downstream API")}</td>
            </tr>
        `).join("");
}

function showMessage(text, type) {
    elements.message.textContent = text;
    elements.message.className = `message visible ${type}`;
}

async function submitOrder(sourceOverride) {
    elements.submit.disabled = true;
    elements.failure.disabled = true;
    elements.message.className = "message";

    const sourceSystem = sourceOverride || document.querySelector("#source-system").value;
    const payload = {
        externalOrderId: document.querySelector("#external-order-id").value,
        sourceSystem,
        customerEmail: document.querySelector("#customer-email").value,
        totalAmount: Number(document.querySelector("#total-amount").value),
        currency: document.querySelector("#currency").value
    };

    try {
        const order = await request("/api/integrations/orders", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        const isFailure = sourceSystem === "DEMO-UNAVAILABLE";
        showMessage(
            isFailure
                ? `Order ${order.externalOrderId} accepted; delivery retried and moved to dead letters.`
                : `Order ${order.externalOrderId} delivered through the ${order.processingLane} lane.`,
            isFailure ? "failure" : "success"
        );
        setInitialOrderId();
        await refreshDashboard();
    } catch (error) {
        showMessage(`Order could not be submitted: ${error.message}`, "failure");
    } finally {
        elements.submit.disabled = false;
        elements.failure.disabled = false;
    }
}

elements.form.addEventListener("submit", event => {
    event.preventDefault();
    submitOrder();
});

elements.failure.addEventListener("click", () => {
    document.querySelector("#external-order-id").value = createOrderId("FAIL");
    submitOrder("DEMO-UNAVAILABLE");
});

elements.refresh.addEventListener("click", () => {
    refreshDashboard().catch(error => showMessage(`Refresh failed: ${error.message}`, "failure"));
});

setInitialOrderId();
refreshDashboard().catch(error => {
    elements.health.textContent = "System unavailable";
    elements.health.className = "badge failure";
    showMessage(`Dashboard could not load: ${error.message}`, "failure");
});
