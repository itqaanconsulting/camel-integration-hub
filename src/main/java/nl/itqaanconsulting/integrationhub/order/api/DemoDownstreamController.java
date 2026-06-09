package nl.itqaanconsulting.integrationhub.order.api;

import nl.itqaanconsulting.integrationhub.order.domain.CanonicalOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/demo/downstream/orders")
public class DemoDownstreamController {

    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@RequestBody CanonicalOrder order) {
        if ("DEMO-UNAVAILABLE".equals(order.sourceSystem())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Demo downstream service is unavailable"));
        }

        return ResponseEntity.accepted()
                .body(Map.of("status", "accepted", "integrationId", order.integrationId().toString()));
    }
}
