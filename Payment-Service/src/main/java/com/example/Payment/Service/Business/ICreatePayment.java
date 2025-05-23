package com.example.Payment.Service.Business;

import com.example.Payment.Service.Domain.PaymentRequest;
import com.stripe.model.PaymentIntent;

public interface ICreatePayment {
     PaymentIntent createPaymentIntent(PaymentRequest request);
     String confirmPaymentIntent(String paymentIntentId);
}
