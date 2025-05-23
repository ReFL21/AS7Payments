package com.example.Payment.Service.Business;

import com.example.Payment.Service.Domain.PaymentRequest;
import com.example.Payment.Service.Events.OrderCreateEvent;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreatePaymentImpl implements ICreatePayment{

    private final RabbitTemplate rabbitTemplate;
    private final String stripeApiKey;

    @Value("${order.exchange:order.exchange}")
    private String exchange;
    @Value("${order.routing.key:order.create}")
    private String routingKey;

    public CreatePaymentImpl(RabbitTemplate rabbitTemplate,@Value("${stripe.api.key}") String stripeApiKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.stripeApiKey   = stripeApiKey;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentIntent createPaymentIntent(PaymentRequest request) {
        try {
            Map<String,Object> params = new HashMap<>();
            params.put("amount", request.getAmount());
            params.put("currency", request.getCurrency());
            params.put("payment_method_types", List.of("card"));
            params.put("metadata", Map.of(
                    "userId",    request.getUserId().toString(),
                    "productIds", String.join(",", request.getProductIds())
            ));

            PaymentIntent intent = PaymentIntent.create(params);
            return intent;
        } catch (StripeException e) {
            throw new RuntimeException("Failed to create PaymentIntent", e);
        }
    }

    @Override
    public String confirmPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent = intent.confirm(Map.of("payment_method", "pm_card_visa"));

            if ("succeeded".equals(intent.getStatus())) {
                Map<String,String> meta = intent.getMetadata();
                Long price     = intent.getAmount();
                List<String> productIds = List.of(meta.get("productIds").split(","));

                OrderCreateEvent evt =  new OrderCreateEvent(true, Instant.now(),Long.valueOf(meta.get("userId")), price, productIds);

                rabbitTemplate.convertAndSend(
                        exchange,
                        routingKey,
                        evt
                );
            }

            return intent.getStatus();
        } catch (StripeException e) {
            throw new RuntimeException("Failed to confirm PaymentIntent", e);
        }
    }
}
