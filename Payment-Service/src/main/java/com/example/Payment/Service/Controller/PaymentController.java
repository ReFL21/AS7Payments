package com.example.Payment.Service.Controller;

import com.example.Payment.Service.Business.ICreatePayment;
import com.example.Payment.Service.Domain.PaymentRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    @Autowired
    private ICreatePayment stripeService;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String,String>> createIntent(
            @RequestBody PaymentRequest request
    ) {
        PaymentIntent intent = stripeService.createPaymentIntent(request);
        return ResponseEntity.ok(Map.of(
                "paymentIntentId", intent.getId(),
                "clientSecret",    intent.getClientSecret()
        ));
    }

    /** Step 2: confirm the PaymentIntent and trigger order creation */
    @PostMapping("/confirm-payment-intent")
    public ResponseEntity<Map<String,String>> confirmIntent(
            @RequestBody Map<String,String> body
    ) {
        String intentId = body.get("paymentIntentId");
        if (intentId == null || intentId.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "paymentIntentId is required"));
        }
        String status = stripeService.confirmPaymentIntent(intentId);
        return ResponseEntity.ok(Map.of("status", status));
    }

}
