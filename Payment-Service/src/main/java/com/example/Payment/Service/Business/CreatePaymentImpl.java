package com.example.Payment.Service.Business;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreatePaymentImpl implements ICreatePayment{

    private final String stripeApiKey;

    public CreatePaymentImpl(@Value("${stripe.api.key}") String stripeApiKey) {
        this.stripeApiKey = stripeApiKey;
    }

    @PostConstruct
    public void init() {
        // Initialize the Stripe API key after Spring has set the property.
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Long amount, String currency) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amount); // amount is in cents: 1000 cents => $10.00
            params.put("currency", currency);
            params.put("payment_method_types", List.of("card"));
            return PaymentIntent.create(params);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }
}
